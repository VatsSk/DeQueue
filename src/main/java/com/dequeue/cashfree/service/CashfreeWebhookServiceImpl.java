package com.dequeue.cashfree.service;

import com.dequeue.cashfree.config.CashfreeProperties;
import com.dequeue.order.entity.Order;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.settlement.entity.PaymentSource;
import com.dequeue.settlement.entity.SettlementStatus;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.repository.VendorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashfreeWebhookServiceImpl implements CashfreeWebhookService {

    private final CashfreeProperties properties;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final CashfreeEasySplitService easySplitService;
    private final FinancialCalculationService financialCalculationService;

    @Override
    public boolean verifySignature(String rawBody, String signature, String timestamp) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            log.warn("Cashfree webhook secret not configured — rejecting webhook");
            return false;
        }
        if (signature == null || timestamp == null) {
            log.warn("Missing signature or timestamp in Cashfree webhook");
            return false;
        }
        try {
            String payload = timestamp + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedBase64 = Base64.getEncoder().encodeToString(computed);
            boolean valid = computedBase64.equals(signature);
            if (!valid) {
                log.warn("Cashfree webhook signature mismatch");
            }
            return valid;
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void processWebhookEvent(String rawBody) {
        try {
            Map<String, Object> event = objectMapper.readValue(rawBody, new TypeReference<>() {});
            String type = (String) event.get("type");
            log.info("Processing Cashfree webhook event type={}", type);

            if (type == null) {
                log.warn("Cashfree webhook received with null type");
                return;
            }

            switch (type) {
                case "PAYMENT_SUCCESS_WEBHOOK" -> handlePaymentSuccess(event);
                case "PAYMENT_FAILED_WEBHOOK" -> handlePaymentFailed(event);
                case "PAYMENT_USER_DROPPED_WEBHOOK" -> handlePaymentDropped(event);
                case "REFUND_STATUS_WEBHOOK" -> handleRefundStatus(event);
                case "VENDOR_EVENTS" -> handleVendorEvent(event);
                default -> log.info("Unhandled Cashfree webhook event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error processing Cashfree webhook: {}", e.getMessage(), e);
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void handlePaymentSuccess(Map<String, Object> event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> order = (Map<String, Object>) data.get("order");
            @SuppressWarnings("unchecked")
            Map<String, Object> payment = (Map<String, Object>) data.get("payment");

            if (order == null || payment == null) {
                log.warn("Payment success webhook missing order or payment data");
                return;
            }

            String cashfreeOrderId = (String) order.get("order_id");
            String cfPaymentId = String.valueOf(payment.get("cf_payment_id"));
            String paymentStatus = (String) payment.get("payment_status");

            log.info("Payment SUCCESS webhook: cashfreeOrderId={}, cfPaymentId={}, status={}",
                    cashfreeOrderId, cfPaymentId, paymentStatus);

            if (!"SUCCESS".equalsIgnoreCase(paymentStatus)) {
                log.info("Payment status not SUCCESS, ignoring. status={}", paymentStatus);
                return;
            }

            // Find DeQueue order by cashfreeOrderId
            // Cashfree order_id = "DQ-" + mongoOrderId
            String mongoOrderId = cashfreeOrderId.startsWith("DQ-")
                    ? cashfreeOrderId.substring(3) : cashfreeOrderId;

            Optional<Order> orderOpt = orderRepository.findById(mongoOrderId);
            if (orderOpt.isEmpty()) {
                // Try by cashfreeOrderId field
                orderOpt = orderRepository.findByCashfreeOrderId(cashfreeOrderId);
            }

            if (orderOpt.isEmpty()) {
                log.error("Cannot find order for cashfreeOrderId={}", cashfreeOrderId);
                return;
            }

            Order dqOrder = orderOpt.get();

            // Idempotency: if already processed with this payment ID, skip
            if (cfPaymentId.equals(dqOrder.getCashfreePaymentId())) {
                log.info("Payment already processed for orderId={}, cfPaymentId={} — skipping",
                        dqOrder.getId(), cfPaymentId);
                return;
            }

            // Calculate financial breakdown
            Vendor vendor = vendorRepository.findById(dqOrder.getVendorId()).orElse(null);
            FinancialCalculationService.FinancialBreakdown breakdown =
                    financialCalculationService.calculateForOrder(dqOrder.getTotalAmount(), vendor);

            // Update order with payment information
            dqOrder.setCashfreePaymentId(cfPaymentId);
            dqOrder.setPaymentSource(PaymentSource.CASHFREE);
            dqOrder.setPlatformFeePercentage(breakdown.platformCommissionRate());
            dqOrder.setPlatformFeeAmount(breakdown.platformCommissionAmount());
            dqOrder.setCashfreeFee(BigDecimal.ZERO); // Will be updated from settlement
            dqOrder.setCashfreeTax(BigDecimal.ZERO);
            dqOrder.setVendorNetAmount(breakdown.vendorGrossShare()); // Temp until CF fees known
            dqOrder.setSettlementStatus(SettlementStatus.PENDING);

            orderRepository.save(dqOrder);
            log.info("Order {} updated with CASHFREE payment. cfPaymentId={}", dqOrder.getId(), cfPaymentId);

            // Trigger Easy Split if vendor is onboarded
            triggerEasySplit(dqOrder, vendor, breakdown);

        } catch (Exception e) {
            log.error("Error handling payment success webhook: {}", e.getMessage(), e);
        }
    }

    private void triggerEasySplit(Order order, Vendor vendor, FinancialCalculationService.FinancialBreakdown breakdown) {
        if (vendor == null || vendor.getCashfreeInfo() == null
                || !vendor.getCashfreeInfo().isEasySplitEnabled()) {
            log.info("Easy Split not enabled for vendor {} — skipping split", order.getVendorId());
            return;
        }

        if (order.getCashfreeOrderId() == null) {
            log.warn("No cashfreeOrderId on order {} — cannot create split", order.getId());
            return;
        }

        // Idempotency: don't create split if already created
        if (order.getCashfreeSplitId() != null) {
            log.info("Easy Split already created for order {} — skipping", order.getId());
            return;
        }

        try {
            String cashfreeVendorId = vendor.getCashfreeInfo().getCashfreeVendorId();
            BigDecimal[] splitAmounts = financialCalculationService.calculateSplitAmounts(
                    order.getTotalAmount(), breakdown.platformCommissionAmount());

            Map<String, Object> splitResponse = easySplitService.createSplitAfterPayment(
                    order.getCashfreeOrderId(), cashfreeVendorId, splitAmounts[0], splitAmounts[1]);

            String splitId = String.valueOf(splitResponse.getOrDefault("split_id",
                    splitResponse.getOrDefault("cashfree_split_id", "")));

            order.setCashfreeSplitId(splitId.isBlank() ? null : splitId);
            orderRepository.save(order);

            log.info("Easy Split created for order {}. splitId={}, vendorAmount={}, platformAmount={}",
                    order.getId(), splitId, splitAmounts[0], splitAmounts[1]);
        } catch (Exception e) {
            log.error("Easy Split creation failed for order {}: {}", order.getId(), e.getMessage());
            // Don't throw — payment is already successful; split can be retried
        }
    }

    private void handlePaymentFailed(Map<String, Object> event) {
        log.info("Payment FAILED webhook received");
        // Could update order status if needed
    }

    private void handlePaymentDropped(Map<String, Object> event) {
        log.info("Payment USER_DROPPED webhook received");
    }

    private void handleRefundStatus(Map<String, Object> event) {
        log.info("Refund status webhook received");
        // Refund handling: update order refundAmount, recalculate vendor net
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            if (data == null) return;

            @SuppressWarnings("unchecked")
            Map<String, Object> refund = (Map<String, Object>) data.get("refund");
            if (refund == null) return;

            String refundStatus = (String) refund.get("refund_status");
            String cashfreeOrderId = (String) refund.get("order_id");
            Object refundAmtObj = refund.get("refund_amount");

            if (!"SUCCESS".equalsIgnoreCase(refundStatus)) return;

            BigDecimal refundAmount = new BigDecimal(String.valueOf(refundAmtObj));
            String mongoOrderId = cashfreeOrderId != null && cashfreeOrderId.startsWith("DQ-")
                    ? cashfreeOrderId.substring(3) : cashfreeOrderId;

            orderRepository.findById(mongoOrderId).ifPresent(order -> {
                BigDecimal currentRefund = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
                order.setRefundAmount(currentRefund.add(refundAmount));
                // Recalculate vendor net
                BigDecimal vendorNet = order.getTotalAmount()
                        .subtract(order.getPlatformFeeAmount())
                        .subtract(order.getCashfreeFee())
                        .subtract(order.getCashfreeTax())
                        .subtract(order.getRefundAmount());
                order.setVendorNetAmount(vendorNet.max(BigDecimal.ZERO));
                orderRepository.save(order);
                log.info("Refund {} applied to order {}", refundAmount, order.getId());
            });
        } catch (Exception e) {
            log.error("Error handling refund webhook: {}", e.getMessage(), e);
        }
    }

    private void handleVendorEvent(Map<String, Object> event) {
        log.info("Vendor event webhook received");
        // Handle vendor status updates from Cashfree
    }
}
