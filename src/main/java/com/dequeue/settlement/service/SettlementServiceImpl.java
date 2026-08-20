package com.dequeue.settlement.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.settlement.dto.*;
import com.dequeue.settlement.entity.*;
import com.dequeue.settlement.repository.PaymentTransactionRepository;
import com.dequeue.settlement.repository.VendorSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central settlement calculation service.
 *
 * <h2>Settlement Formula</h2>
 * <pre>
 * Total Eligible Order Amount
 *         - Cashfree Fees       (only on Cashfree transactions)
 *         - Cashfree Tax        (only on Cashfree transactions)
 *         - DeQueue Platform Charges (on ALL transactions)
 *         - Refunds
 *         = Vendor Net Payable
 *
 * Vendor Net Payable
 *         - Previously Settled Amount
 *         = Amount Currently Due
 * </pre>
 *
 * <h2>Financial Integrity</h2>
 * <ul>
 *   <li>Fee snapshots are written once when a transaction is finalized.</li>
 *   <li>If platform commission changes, historical transactions are unaffected.</li>
 *   <li>Cashfree fees are NEVER charged on cash/offline transactions.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    // ── Default platform fee percentage ──────────────────────────────────────
    // In a production implementation this should come from vendor-specific config
    // or a PlatformConfig entity. Kept as a constant for now and clearly documented.
    private static final BigDecimal DEFAULT_PLATFORM_FEE_PERCENTAGE = new BigDecimal("5.00");

    // ── Default Cashfree fee percentage (2% example) ─────────────────────────
    // In production: read from Cashfree webhook / settlement API or vendor config.
    private static final BigDecimal DEFAULT_CASHFREE_FEE_PERCENTAGE = new BigDecimal("2.00");

    // ── Default Cashfree tax percentage (18% GST on gateway fee) ─────────────
    private static final BigDecimal DEFAULT_CASHFREE_TAX_PERCENTAGE = new BigDecimal("18.00");

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final VendorSettlementRepository settlementRepository;
    private final com.dequeue.vendor.repository.VendorRepository vendorRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public SettlementSummaryResponse getSummary(String vendorId) {
        List<PaymentTransaction> allTransactions = transactionRepository.findByVendorId(vendorId);

        // Separate completed (eligible) transactions
        List<PaymentTransaction> eligible = allTransactions.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(Collectors.toList());

        BigDecimal cashfreeSales = sum(eligible, t -> t.getPaymentSource() == PaymentSource.CASHFREE ? t.getAmount() : BigDecimal.ZERO);
        BigDecimal cashSales = sum(eligible, t -> t.getPaymentSource() == PaymentSource.CASH ? t.getAmount() : BigDecimal.ZERO);
        BigDecimal offlineSales = sum(eligible, t -> t.getPaymentSource() == PaymentSource.OFFLINE ? t.getAmount() : BigDecimal.ZERO);
        BigDecimal totalSales = cashfreeSales.add(cashSales).add(offlineSales);

        BigDecimal totalCashfreeFees = sum(eligible, PaymentTransaction::getCashfreeFee);
        BigDecimal totalCashfreeTax = sum(eligible, PaymentTransaction::getCashfreeTax);
        BigDecimal totalPlatformCharges = sum(eligible, PaymentTransaction::getPlatformFeeAmount);
        BigDecimal totalRefunds = sum(eligible, PaymentTransaction::getRefundAmount);

        BigDecimal totalVendorEarnings = totalSales
                .subtract(totalCashfreeFees)
                .subtract(totalCashfreeTax)
                .subtract(totalPlatformCharges)
                .subtract(totalRefunds);

        // Already settled = sum of all SETTLED settlements' net amounts
        List<VendorSettlement> settledBatches = settlementRepository
                .findByVendorIdAndSettlementStatus(vendorId, SettlementStatus.SETTLED);
        BigDecimal alreadySettled = settledBatches.stream()
                .map(VendorSettlement::getNetSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingSettlement = totalVendorEarnings.subtract(alreadySettled);
        if (pendingSettlement.compareTo(BigDecimal.ZERO) < 0) {
            pendingSettlement = BigDecimal.ZERO;
        }

        // Settled till info
        Optional<VendorSettlement> lastSettled = settlementRepository
                .findTopByVendorIdAndSettlementStatusOrderByPeriodToDesc(vendorId, SettlementStatus.SETTLED);

        LocalDate settledTill = lastSettled.map(VendorSettlement::getPeriodTo).orElse(null);
        LocalDate pendingFrom = null;
        if (settledTill != null) {
            pendingFrom = settledTill.plusDays(1);
        } else {
            // No settlements — pending from the first eligible transaction
            eligible.stream()
                    .map(PaymentTransaction::getRecordedAt)
                    .min(Instant::compareTo)
                    .ifPresent(t -> {
                        // pendingFrom is set below — work around lambda effectively-final
                    });
        }

        // Find pending from date from first pending transaction
        if (pendingFrom == null) {
            List<PaymentTransaction> pending = eligible.stream()
                    .filter(t -> t.getSettlementStatus() == SettlementStatus.PENDING)
                    .collect(Collectors.toList());
            pendingFrom = pending.stream()
                    .map(PaymentTransaction::getRecordedAt)
                    .min(Instant::compareTo)
                    .map(i -> i.atZone(ZoneId.systemDefault()).toLocalDate())
                    .orElse(null);
        }

        long settledOrderCount = eligible.stream()
                .filter(t -> t.getSettlementStatus() == SettlementStatus.SETTLED)
                .count();
        long pendingOrderCount = eligible.stream()
                .filter(t -> t.getSettlementStatus() == SettlementStatus.PENDING)
                .count();

        return SettlementSummaryResponse.builder()
                .totalSales(totalSales)
                .cashfreeSales(cashfreeSales)
                .cashSales(cashSales)
                .offlineSales(offlineSales)
                .totalCashfreeFees(totalCashfreeFees)
                .totalCashfreeTax(totalCashfreeTax)
                .totalPlatformCharges(totalPlatformCharges)
                .totalRefunds(totalRefunds)
                .totalVendorEarnings(totalVendorEarnings)
                .alreadySettled(alreadySettled)
                .pendingSettlement(pendingSettlement)
                .settledTillDate(settledTill)
                .lastSettlementRef(lastSettled.map(VendorSettlement::getSettlementRef).orElse(null))
                .lastSettlementAmount(lastSettled.map(VendorSettlement::getNetSettlementAmount).orElse(null))
                .lastSettlementDate(lastSettled.map(VendorSettlement::getSettledAt).orElse(null))
                .pendingFrom(pendingFrom)
                .totalOrders(eligible.size())
                .settledOrders((int) settledOrderCount)
                .pendingOrders((int) pendingOrderCount)
                .build();
    }

    @Override
    public PageResponse<SettlementResponse> getSettlementHistory(String vendorId, int page, int size) {
        Page<VendorSettlement> pageResult = settlementRepository.findByVendorIdOrderByCreatedAtDesc(
                vendorId, PageRequest.of(page, size));

        List<SettlementResponse> content = pageResult.getContent().stream()
                .map(this::toSettlementResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(content, pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
    }

    @Override
    public SettlementDetailResponse getSettlementDetail(String vendorId, String settlementId) {
        VendorSettlement settlement = settlementRepository.findById(settlementId)
                .filter(s -> s.getVendorId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));

        List<PaymentTransaction> txns = transactionRepository.findBySettlementId(settlementId);

        return SettlementDetailResponse.builder()
                .id(settlement.getId())
                .settlementRef(settlement.getSettlementRef())
                .periodFrom(settlement.getPeriodFrom())
                .periodTo(settlement.getPeriodTo())
                .orderCount(settlement.getOrderCount())
                .cashfreeSales(settlement.getCashfreeSales())
                .offlineSales(settlement.getOfflineSales())
                .totalSales(settlement.getTotalSales())
                .totalCashfreeFees(settlement.getTotalCashfreeFees())
                .totalCashfreeTax(settlement.getTotalCashfreeTax())
                .totalPlatformCharges(settlement.getTotalPlatformCharges())
                .totalRefunds(settlement.getTotalRefunds())
                .netSettlementAmount(settlement.getNetSettlementAmount())
                .settlementStatus(settlement.getSettlementStatus())
                .settledAt(settlement.getSettledAt())
                .createdAt(settlement.getCreatedAt())
                .adminNotes(settlement.getAdminNotes())
                .transactions(txns.stream().map(this::toLedgerEntry).collect(Collectors.toList()))
                .build();
    }

    @Override
    public PageResponse<TransactionLedgerEntry> getTransactionLedger(
            String vendorId, LocalDate from, LocalDate to, int page, int size) {

        Page<PaymentTransaction> pageResult;
        if (from != null && to != null) {
            // Filter by date range using a pageable query via MongoRepository derived method
            // We use a full list + manual slice here because MongoRepository doesn't support
            // pageable + date range with a single derived method without @Query.
            // For production at scale, add a @Query or use MongoTemplate.
            List<PaymentTransaction> filtered = transactionRepository.findByVendorIdAndRecordedAtBetween(
                    vendorId, toStartOfDay(from), toEndOfDay(to));
            return toManualPage(filtered.stream().map(this::toLedgerEntry).collect(Collectors.toList()),
                    page, size, filtered.size());
        } else {
            pageResult = transactionRepository.findByVendorId(
                    vendorId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt")));
            List<TransactionLedgerEntry> content = pageResult.getContent().stream()
                    .map(this::toLedgerEntry).collect(Collectors.toList());
            return new PageResponse<>(content, pageResult.getNumber(), pageResult.getSize(),
                    pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
        }
    }

    @Override
    public TransactionLedgerEntry getTransaction(String vendorId, String transactionId) {
        PaymentTransaction txn = transactionRepository.findById(transactionId)
                .filter(t -> t.getVendorId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        return toLedgerEntry(txn);
    }

    @Override
    public FinancialReportResponse getFinancialReport(String vendorId, LocalDate from, LocalDate to) {
        List<PaymentTransaction> txns = transactionRepository.findByVendorIdAndRecordedAtBetween(
                vendorId, toStartOfDay(from), toEndOfDay(to));

        List<PaymentTransaction> eligible = txns.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(Collectors.toList());

        List<PaymentTransaction> cashfreeTxns = eligible.stream()
                .filter(t -> t.getPaymentSource() == PaymentSource.CASHFREE)
                .collect(Collectors.toList());
        List<PaymentTransaction> cashTxns = eligible.stream()
                .filter(t -> t.getPaymentSource() == PaymentSource.CASH)
                .collect(Collectors.toList());
        List<PaymentTransaction> offlineTxns = eligible.stream()
                .filter(t -> t.getPaymentSource() == PaymentSource.OFFLINE
                        || t.getPaymentSource() == PaymentSource.OTHER)
                .collect(Collectors.toList());

        BigDecimal cashfreeSales = sum(cashfreeTxns, PaymentTransaction::getAmount);
        BigDecimal cashSales = sum(cashTxns, PaymentTransaction::getAmount);
        BigDecimal offlineSales = sum(offlineTxns, PaymentTransaction::getAmount);
        BigDecimal totalSales = cashfreeSales.add(cashSales).add(offlineSales);

        BigDecimal cashfreeFees = sum(eligible, PaymentTransaction::getCashfreeFee);
        BigDecimal cashfreeTax = sum(eligible, PaymentTransaction::getCashfreeTax);
        BigDecimal platformCharges = sum(eligible, PaymentTransaction::getPlatformFeeAmount);
        BigDecimal refunds = sum(eligible, PaymentTransaction::getRefundAmount);

        BigDecimal vendorNetPayable = totalSales
                .subtract(cashfreeFees)
                .subtract(cashfreeTax)
                .subtract(platformCharges)
                .subtract(refunds);

        // Settlement info for period
        BigDecimal periodSettled = eligible.stream()
                .filter(t -> t.getSettlementStatus() == SettlementStatus.SETTLED)
                .map(PaymentTransaction::getVendorNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodPending = eligible.stream()
                .filter(t -> t.getSettlementStatus() == SettlementStatus.PENDING)
                .map(PaymentTransaction::getVendorNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Online breakdown
        OnlinePaymentSummary online = OnlinePaymentSummary.builder()
                .orderCount(cashfreeTxns.size())
                .grossAmount(cashfreeSales)
                .cashfreeFees(sum(cashfreeTxns, PaymentTransaction::getCashfreeFee))
                .cashfreeTax(sum(cashfreeTxns, PaymentTransaction::getCashfreeTax))
                .platformFees(sum(cashfreeTxns, PaymentTransaction::getPlatformFeeAmount))
                .refunds(sum(cashfreeTxns, PaymentTransaction::getRefundAmount))
                .vendorNetAmount(sum(cashfreeTxns, PaymentTransaction::getVendorNetAmount))
                .build();

        // Offline breakdown
        OfflinePaymentSummary offline = OfflinePaymentSummary.builder()
                .cashOrderCount(cashTxns.size())
                .offlineOrderCount(offlineTxns.size())
                .cashAmount(cashSales)
                .offlineAmount(offlineSales)
                .grossAmount(cashSales.add(offlineSales))
                .cashfreeFees(BigDecimal.ZERO) // Explicitly zero — never charged on offline
                .platformFees(sum(cashTxns, PaymentTransaction::getPlatformFeeAmount)
                        .add(sum(offlineTxns, PaymentTransaction::getPlatformFeeAmount)))
                .refunds(sum(cashTxns, PaymentTransaction::getRefundAmount)
                        .add(sum(offlineTxns, PaymentTransaction::getRefundAmount)))
                .vendorNetAmount(sum(cashTxns, PaymentTransaction::getVendorNetAmount)
                        .add(sum(offlineTxns, PaymentTransaction::getVendorNetAmount)))
                .build();

        // Reconciliation
        ReconciliationResponse reconciliation = ReconciliationResponse.builder()
                .totalEligibleOrders(eligible.size())
                .totalEligibleRevenue(totalSales)
                .totalCashfreeFees(cashfreeFees)
                .totalCashfreeTax(cashfreeTax)
                .totalPlatformCharges(platformCharges)
                .totalRefunds(refunds)
                .totalVendorNetPayable(vendorNetPayable)
                .alreadySettled(periodSettled)
                .remainingPendingSettlement(periodPending)
                .build();

        return FinancialReportResponse.builder()
                .fromDate(from)
                .toDate(to)
                .totalSales(totalSales)
                .cashfreeSales(cashfreeSales)
                .cashSales(cashSales)
                .offlineSales(offlineSales)
                .cashfreeFees(cashfreeFees)
                .cashfreeTax(cashfreeTax)
                .platformCharges(platformCharges)
                .refunds(refunds)
                .vendorNetPayable(vendorNetPayable)
                .alreadySettled(periodSettled)
                .pendingSettlement(periodPending)
                .onlineBreakdown(online)
                .offlineBreakdown(offline)
                .reconciliation(reconciliation)
                .totalTransactions(eligible.size())
                .transactions(eligible.stream().map(this::toLedgerEntry).collect(Collectors.toList()))
                .build();
    }

    @Override
    public PendingSettlementResponse getPendingSettlement(String vendorId) {
        List<PaymentTransaction> pending = transactionRepository
                .findByVendorIdAndSettlementStatus(vendorId, SettlementStatus.PENDING)
                .stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(Collectors.toList());

        BigDecimal cashfreeSales = sum(pending, t -> t.getPaymentSource() == PaymentSource.CASHFREE
                ? t.getAmount() : BigDecimal.ZERO);
        BigDecimal offlineSales = sum(pending, t -> t.getPaymentSource() != PaymentSource.CASHFREE
                ? t.getAmount() : BigDecimal.ZERO);
        BigDecimal grossSales = cashfreeSales.add(offlineSales);

        BigDecimal cashreeFees = sum(pending, PaymentTransaction::getCashfreeFee);
        BigDecimal cashreeTax = sum(pending, PaymentTransaction::getCashfreeTax);
        BigDecimal platformCharges = sum(pending, PaymentTransaction::getPlatformFeeAmount);
        BigDecimal refunds = sum(pending, PaymentTransaction::getRefundAmount);
        BigDecimal pendingAmount = sum(pending, PaymentTransaction::getVendorNetAmount);

        // Pending from = earliest pending transaction date
        LocalDate pendingFrom = pending.stream()
                .map(PaymentTransaction::getRecordedAt)
                .min(Instant::compareTo)
                .map(i -> i.atZone(ZoneId.systemDefault()).toLocalDate())
                .orElse(null);

        return PendingSettlementResponse.builder()
                .pendingFrom(pendingFrom)
                .pendingOrderCount(pending.size())
                .grossSales(grossSales)
                .cashfreeSales(cashfreeSales)
                .offlineSales(offlineSales)
                .cashreeFees(cashreeFees)
                .cashreeTax(cashreeTax)
                .platformCharges(platformCharges)
                .refunds(refunds)
                .pendingAmount(pendingAmount)
                .pendingTransactions(pending.stream().map(this::toLedgerEntry).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public TransactionLedgerEntry recordOfflinePayment(
            String vendorId, String orderId, OfflinePaymentRequest request,
            String staffId, String staffName) {

        // 1. Validate order exists and belongs to this vendor
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getVendorId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // 2. Only COMPLETED orders are eligible for settlement
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException(
                    "Only COMPLETED orders can have offline payments recorded. Order status: " + order.getStatus());
        }

        // 3. Reject duplicate payment attempts
        if (order.getPaymentTransactionId() != null) {
            throw new BadRequestException(
                    "A payment transaction already exists for order " + orderId +
                    ". Transaction ID: " + order.getPaymentTransactionId());
        }
        if (transactionRepository.existsByOrderIdAndPaymentStatus(orderId, PaymentStatus.COMPLETED)) {
            throw new BadRequestException("Duplicate payment attempt. Order " + orderId + " already has a completed payment.");
        }

        // 4. Validate and parse payment source
        PaymentSource source;
        try {
            source = PaymentSource.valueOf(request.getPaymentSource().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid paymentSource. Allowed: CASH, OFFLINE, OTHER");
        }
        if (source == PaymentSource.CASHFREE) {
            throw new BadRequestException("Cannot record CASHFREE payment as offline. Use the Cashfree payment flow.");
        }

        // 5. Calculate fee snapshot at time of finalization
        com.dequeue.vendor.entity.Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));
                
        BigDecimal platformFeePercentage = DEFAULT_PLATFORM_FEE_PERCENTAGE;
        if (vendor.getSettings() != null && vendor.getSettings().getPlatformFeePercentage() != null) {
            platformFeePercentage = vendor.getSettings().getPlatformFeePercentage();
        }
        
        BigDecimal amount = request.getAmount();
        BigDecimal platformFeeAmount = amount
                .multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        // Cashfree fees = ₹0 for offline payments (by design)
        BigDecimal cashfreeFee = BigDecimal.ZERO;
        BigDecimal cashfreeTax = BigDecimal.ZERO;
        BigDecimal refundAmount = BigDecimal.ZERO;

        BigDecimal vendorNetAmount = amount
                .subtract(platformFeeAmount)
                .subtract(refundAmount);

        // 6. Create PaymentTransaction
        PaymentTransaction txn = PaymentTransaction.builder()
                .orderId(orderId)
                .vendorId(vendorId)
                .paymentId(generateOfflinePaymentId(orderId))
                .paymentSource(source)
                .amount(amount)
                .paymentStatus(PaymentStatus.COMPLETED)
                .cashfreeFee(cashfreeFee)
                .cashfreeTax(cashfreeTax)
                .platformFeePercentage(platformFeePercentage)
                .platformFeeAmount(platformFeeAmount)
                .refundAmount(refundAmount)
                .vendorNetAmount(vendorNetAmount)
                .settlementStatus(SettlementStatus.PENDING)
                .recordedBy(staffId)
                .recordedByName(staffName)
                .notes(request.getNotes())
                .reference(request.getReference())
                .build();

        txn = transactionRepository.save(txn);
        log.info("Recorded offline payment for order {} by staff {}: source={}, amount={}",
                orderId, staffId, source, amount);

        // 7. Snapshot financial fields on the Order (immutable after this point)
        order.setPaymentSource(source);
        order.setPaymentTransactionId(txn.getId());
        order.setPlatformFeePercentage(platformFeePercentage);
        order.setPlatformFeeAmount(platformFeeAmount);
        order.setCashfreeFee(cashfreeFee);
        order.setCashfreeTax(cashfreeTax);
        order.setRefundAmount(refundAmount);
        order.setVendorNetAmount(vendorNetAmount);
        order.setSettlementStatus(SettlementStatus.PENDING);
        orderRepository.save(order);

        return toLedgerEntry(txn);
    }

    @Override
    public String exportFinancialReportCsv(String vendorId, LocalDate from, LocalDate to) {
        FinancialReportResponse report = getFinancialReport(vendorId, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("Order ID,Date,Payment Source,Order Amount,Cashfree Amount," +
                "Cashfree Fee,Cashfree Tax,Platform Fee %,Platform Fee Amount," +
                "Refund,Vendor Net Amount,Settlement Status,Settlement ID,Settled Date\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        for (TransactionLedgerEntry e : report.getTransactions()) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    csvEscape(e.getOrderId()),
                    e.getOrderDate() != null ? fmt.format(e.getOrderDate()) : "",
                    e.getPaymentSource(),
                    e.getOrderAmount(),
                    e.getPaymentSource() == PaymentSource.CASHFREE ? e.getOrderAmount() : "0",
                    e.getCashfreeFee(),
                    e.getCashfreeTax(),
                    e.getPlatformFeePercentage(),
                    e.getPlatformFeeAmount(),
                    e.getRefundAmount(),
                    e.getVendorNetAmount(),
                    e.getSettlementStatus(),
                    e.getSettlementId() != null ? e.getSettlementId() : "",
                    e.getSettledAt() != null ? fmt.format(e.getSettledAt()) : ""
            ));
        }

        return csv.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    private TransactionLedgerEntry toLedgerEntry(PaymentTransaction t) {
        // Look up settlement ref from settlement id if available
        String settlementRef = null;
        if (t.getSettlementId() != null) {
            settlementRef = settlementRepository.findById(t.getSettlementId())
                    .map(VendorSettlement::getSettlementRef)
                    .orElse(null);
        }

        // Look up order date from order (orderId stored on transaction)
        Instant orderDate = null;
        String queueNumber = null;
        try {
            Optional<Order> order = orderRepository.findById(t.getOrderId());
            if (order.isPresent()) {
                orderDate = order.get().getCreatedAt();
                queueNumber = order.get().getQueueNumber();
            }
        } catch (Exception ex) {
            log.warn("Could not load order {} for ledger entry", t.getOrderId());
        }

        return TransactionLedgerEntry.builder()
                .transactionId(t.getId())
                .orderId(t.getOrderId())
                .paymentId(t.getPaymentId())
                .orderDate(orderDate)
                .queueNumber(queueNumber)
                .paymentSource(t.getPaymentSource())
                .paymentStatus(t.getPaymentStatus())
                .orderAmount(t.getAmount())
                .cashfreeFee(t.getCashfreeFee())
                .cashfreeTax(t.getCashfreeTax())
                .platformFeePercentage(t.getPlatformFeePercentage())
                .platformFeeAmount(t.getPlatformFeeAmount())
                .refundAmount(t.getRefundAmount())
                .vendorNetAmount(t.getVendorNetAmount())
                .settlementStatus(t.getSettlementStatus())
                .settlementId(t.getSettlementId())
                .settlementRef(settlementRef)
                .settledAt(t.getSettledAt())
                .recordedBy(t.getRecordedBy())
                .recordedByName(t.getRecordedByName())
                .recordedAt(t.getRecordedAt())
                .notes(t.getNotes())
                .reference(t.getReference())
                .build();
    }

    private SettlementResponse toSettlementResponse(VendorSettlement s) {
        return SettlementResponse.builder()
                .id(s.getId())
                .settlementRef(s.getSettlementRef())
                .periodFrom(s.getPeriodFrom())
                .periodTo(s.getPeriodTo())
                .orderCount(s.getOrderCount())
                .cashfreeSales(s.getCashfreeSales())
                .offlineSales(s.getOfflineSales())
                .totalSales(s.getTotalSales())
                .totalCashfreeFees(s.getTotalCashfreeFees())
                .totalCashfreeTax(s.getTotalCashfreeTax())
                .totalPlatformCharges(s.getTotalPlatformCharges())
                .totalRefunds(s.getTotalRefunds())
                .netSettlementAmount(s.getNetSettlementAmount())
                .settlementStatus(s.getSettlementStatus())
                .settledAt(s.getSettledAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    /**
     * Sums a BigDecimal field across a list, treating null values as ZERO.
     */
    private <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> extractor) {
        return list.stream()
                .map(extractor)
                .map(v -> v != null ? v : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Instant toStartOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant toEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
    }

    private String generateOfflinePaymentId(String orderId) {
        return "OFF-" + orderId.substring(Math.max(0, orderId.length() - 8)).toUpperCase()
                + "-" + Instant.now().toEpochMilli();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Manual pagination helper for when we can't use Spring Data pageable directly
     * (e.g., when filtering in-memory after a list query).
     */
    private <T> PageResponse<T> toManualPage(List<T> all, int page, int size, int total) {
        int fromIdx = page * size;
        int toIdx = Math.min(fromIdx + size, all.size());
        List<T> slice = (fromIdx < all.size()) ? all.subList(fromIdx, toIdx) : new ArrayList<>();
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponse<>(slice, page, size, total, totalPages, page >= totalPages - 1);
    }
}
