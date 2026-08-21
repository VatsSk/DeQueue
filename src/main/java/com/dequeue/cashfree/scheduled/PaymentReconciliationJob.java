package com.dequeue.cashfree.scheduled;

import com.dequeue.cashfree.service.CashfreeEasySplitService;
import com.dequeue.cashfree.service.CashfreePaymentService;
import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.settlement.entity.PaymentSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Scheduled reconciliation job that periodically syncs payment/split status
 * from Cashfree for orders where payment information is incomplete.
 *
 * Enabled via: cashfree.reconciliation.enabled=true
 * Cron via:    cashfree.reconciliation.cron
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "cashfree.reconciliation.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final OrderRepository orderRepository;
    private final CashfreePaymentService cashfreePaymentService;
    private final CashfreeEasySplitService easySplitService;

    /**
     * Reconciliation job — runs on configurable cron schedule.
     * Default: every day at 2:00 AM.
     */
    @Scheduled(cron = "${cashfree.reconciliation.cron:0 0 2 * * ?}")
    public void reconcile() {
        log.info("Starting Cashfree payment reconciliation job");

        try {
            // Find online orders missing payment ID (possibly unconfirmed payments)
            List<Order> missingPaymentId = orderRepository.findAll().stream()
                    .filter(o -> o.getPaymentSource() == PaymentSource.CASHFREE)
                    .filter(o -> o.getCashfreePaymentId() == null)
                    .filter(o -> o.getCashfreeOrderId() != null)
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                    .limit(50) // Process max 50 per run
                    .toList();

            log.info("Found {} orders missing Cashfree payment ID", missingPaymentId.size());

            for (Order order : missingPaymentId) {
                try {
                    Map<String, Object> statusResult = cashfreePaymentService.getPaymentStatus(
                            order.getCashfreeOrderId());
                    log.info("Reconciliation check for order {}: status={}",
                            order.getId(), statusResult.get("order_status"));
                    // Status comparison and update logic would go here
                } catch (Exception e) {
                    log.warn("Reconciliation failed for order {}: {}", order.getId(), e.getMessage());
                }
            }

            // Find online orders missing split ID
            List<Order> missingSplitId = orderRepository.findAll().stream()
                    .filter(o -> o.getPaymentSource() == PaymentSource.CASHFREE)
                    .filter(o -> o.getCashfreePaymentId() != null) // Payment confirmed
                    .filter(o -> o.getCashfreeSplitId() == null) // Split missing
                    .filter(o -> o.getCashfreeOrderId() != null)
                    .limit(50)
                    .toList();

            log.info("Found {} orders missing Easy Split ID", missingSplitId.size());
            // Split retry logic would go here

            log.info("Cashfree reconciliation job completed");
        } catch (Exception e) {
            log.error("Cashfree reconciliation job failed: {}", e.getMessage(), e);
        }
    }
}
