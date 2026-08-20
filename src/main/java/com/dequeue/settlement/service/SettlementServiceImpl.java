package com.dequeue.settlement.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.settlement.dto.*;
import com.dequeue.settlement.entity.*;
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
    private final VendorSettlementRepository settlementRepository;
    private final com.dequeue.vendor.repository.VendorRepository vendorRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public SettlementSummaryResponse getSummary(String vendorId) {
        // Get all COMPLETED orders for this vendor
        List<Order> allOrders = orderRepository.findByVendorIdAndStatus(vendorId, OrderStatus.COMPLETED);

        // Filter orders that have payment recorded
        List<Order> paidOrders = allOrders.stream()
                .filter(o -> o.getPaymentSource() != null)
                .collect(Collectors.toList());

        BigDecimal cashfreeSales = sumOrders(paidOrders, o -> o.getPaymentSource() == PaymentSource.CASHFREE ? o.getTotalAmount() : BigDecimal.ZERO);
        BigDecimal cashSales = sumOrders(paidOrders, o -> o.getPaymentSource() == PaymentSource.CASH ? o.getTotalAmount() : BigDecimal.ZERO);
        BigDecimal offlineSales = sumOrders(paidOrders, o -> (o.getPaymentSource() == PaymentSource.OFFLINE || o.getPaymentSource() == PaymentSource.OTHER) ? o.getTotalAmount() : BigDecimal.ZERO);
        BigDecimal totalSales = cashfreeSales.add(cashSales).add(offlineSales);

        BigDecimal totalCashfreeFees = sumOrders(paidOrders, Order::getCashfreeFee);
        BigDecimal totalCashfreeTax = sumOrders(paidOrders, Order::getCashfreeTax);
        BigDecimal totalPlatformCharges = sumOrders(paidOrders, Order::getPlatformFeeAmount);
        BigDecimal totalRefunds = sumOrders(paidOrders, Order::getRefundAmount);

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
        }

        // Find pending from date from first pending order
        if (pendingFrom == null) {
            List<Order> pending = paidOrders.stream()
                    .filter(o -> o.getSettlementStatus() == SettlementStatus.PENDING)
                    .collect(Collectors.toList());
            pendingFrom = pending.stream()
                    .map(Order::getCreatedAt)
                    .min(Instant::compareTo)
                    .map(i -> i.atZone(ZoneId.systemDefault()).toLocalDate())
                    .orElse(null);
        }

        long settledOrderCount = paidOrders.stream()
                .filter(o -> o.getSettlementStatus() == SettlementStatus.SETTLED)
                .count();
        long pendingOrderCount = paidOrders.stream()
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
                .totalOrders(paidOrders.size())
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

        List<Order> orders = orderRepository.findBySettlementId(settlementId);

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
                .transactions(orders.stream().map(this::toLedgerEntry).collect(Collectors.toList()))
                .build();
    }

    @Override
    public PageResponse<TransactionLedgerEntry> getTransactionLedger(
            String vendorId, LocalDate from, LocalDate to, int page, int size) {

        if (from != null && to != null) {
            // Filter by date range - get COMPLETED orders with payment recorded
            List<Order> filtered = orderRepository.findByVendorIdAndStatusAndCreatedAtBetween(
                    vendorId, OrderStatus.COMPLETED, toStartOfDay(from), toEndOfDay(to));
            List<Order> paidOrders = filtered.stream()
                    .filter(o -> o.getPaymentSource() != null)
                    .collect(Collectors.toList());
            return toManualPage(paidOrders.stream().map(this::toLedgerEntry).collect(Collectors.toList()),
                    page, size, paidOrders.size());
        } else {
            // Get all COMPLETED orders with pagination
            Page<Order> pageResult = orderRepository.findByVendorIdAndStatus(
                    vendorId, OrderStatus.COMPLETED, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
            List<TransactionLedgerEntry> content = pageResult.getContent().stream()
                    .filter(o -> o.getPaymentSource() != null)
                    .map(this::toLedgerEntry).collect(Collectors.toList());
            return new PageResponse<>(content, pageResult.getNumber(), pageResult.getSize(),
                    pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.isLast());
        }
    }

    @Override
    public TransactionLedgerEntry getTransaction(String vendorId, String transactionId) {
        // transactionId is actually orderId since we don't have separate transactions
        Order order = orderRepository.findById(transactionId)
                .filter(o -> o.getVendorId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        return toLedgerEntry(order);
    }

    @Override
    public FinancialReportResponse getFinancialReport(String vendorId, LocalDate from, LocalDate to) {
        List<Order> orders = orderRepository.findByVendorIdAndStatusAndCreatedAtBetween(
                vendorId, OrderStatus.COMPLETED, toStartOfDay(from), toEndOfDay(to));

        List<Order> paidOrders = orders.stream()
                .filter(o -> o.getPaymentSource() != null)
                .collect(Collectors.toList());

        List<Order> cashfreeOrders = paidOrders.stream()
                .filter(o -> o.getPaymentSource() == PaymentSource.CASHFREE)
                .collect(Collectors.toList());
        List<Order> cashOrders = paidOrders.stream()
                .filter(o -> o.getPaymentSource() == PaymentSource.CASH)
                .collect(Collectors.toList());
        List<Order> offlineOrders = paidOrders.stream()
                .filter(o -> o.getPaymentSource() == PaymentSource.OFFLINE
                        || o.getPaymentSource() == PaymentSource.OTHER)
                .collect(Collectors.toList());

        BigDecimal cashfreeSales = sumOrders(cashfreeOrders, Order::getTotalAmount);
        BigDecimal cashSales = sumOrders(cashOrders, Order::getTotalAmount);
        BigDecimal offlineSales = sumOrders(offlineOrders, Order::getTotalAmount);
        BigDecimal totalSales = cashfreeSales.add(cashSales).add(offlineSales);

        BigDecimal cashfreeFees = sumOrders(paidOrders, Order::getCashfreeFee);
        BigDecimal cashfreeTax = sumOrders(paidOrders, Order::getCashfreeTax);
        BigDecimal platformCharges = sumOrders(paidOrders, Order::getPlatformFeeAmount);
        BigDecimal refunds = sumOrders(paidOrders, Order::getRefundAmount);

        BigDecimal vendorNetPayable = totalSales
                .subtract(cashfreeFees)
                .subtract(cashfreeTax)
                .subtract(platformCharges)
                .subtract(refunds);

        // Settlement info for period
        BigDecimal periodSettled = paidOrders.stream()
                .filter(o -> o.getSettlementStatus() == SettlementStatus.SETTLED)
                .map(Order::getVendorNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal periodPending = paidOrders.stream()
                .filter(o -> o.getSettlementStatus() == SettlementStatus.PENDING)
                .map(Order::getVendorNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Online breakdown
        OnlinePaymentSummary online = OnlinePaymentSummary.builder()
                .orderCount(cashfreeOrders.size())
                .grossAmount(cashfreeSales)
                .cashfreeFees(sumOrders(cashfreeOrders, Order::getCashfreeFee))
                .cashfreeTax(sumOrders(cashfreeOrders, Order::getCashfreeTax))
                .platformFees(sumOrders(cashfreeOrders, Order::getPlatformFeeAmount))
                .refunds(sumOrders(cashfreeOrders, Order::getRefundAmount))
                .vendorNetAmount(sumOrders(cashfreeOrders, Order::getVendorNetAmount))
                .build();

        // Offline breakdown
        OfflinePaymentSummary offline = OfflinePaymentSummary.builder()
                .cashOrderCount(cashOrders.size())
                .offlineOrderCount(offlineOrders.size())
                .cashAmount(cashSales)
                .offlineAmount(offlineSales)
                .grossAmount(cashSales.add(offlineSales))
                .cashfreeFees(BigDecimal.ZERO) // Explicitly zero — never charged on offline
                .platformFees(sumOrders(cashOrders, Order::getPlatformFeeAmount)
                        .add(sumOrders(offlineOrders, Order::getPlatformFeeAmount)))
                .refunds(sumOrders(cashOrders, Order::getRefundAmount)
                        .add(sumOrders(offlineOrders, Order::getRefundAmount)))
                .vendorNetAmount(sumOrders(cashOrders, Order::getVendorNetAmount)
                        .add(sumOrders(offlineOrders, Order::getVendorNetAmount)))
                .build();

        // Reconciliation
        ReconciliationResponse reconciliation = ReconciliationResponse.builder()
                .totalEligibleOrders(paidOrders.size())
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
                .totalTransactions(paidOrders.size())
                .transactions(paidOrders.stream().map(this::toLedgerEntry).collect(Collectors.toList()))
                .build();
    }

    @Override
    public PendingSettlementResponse getPendingSettlement(String vendorId) {
        List<Order> pendingOrders = orderRepository
                .findByVendorIdAndStatusAndSettlementStatus(vendorId, OrderStatus.COMPLETED, SettlementStatus.PENDING)
                .stream()
                .filter(o -> o.getPaymentSource() != null)
                .collect(Collectors.toList());

        BigDecimal cashfreeSales = sumOrders(pendingOrders, o -> o.getPaymentSource() == PaymentSource.CASHFREE
                ? o.getTotalAmount() : BigDecimal.ZERO);
        BigDecimal offlineSales = sumOrders(pendingOrders, o -> o.getPaymentSource() != PaymentSource.CASHFREE
                ? o.getTotalAmount() : BigDecimal.ZERO);
        BigDecimal grossSales = cashfreeSales.add(offlineSales);

        BigDecimal cashreeFees = sumOrders(pendingOrders, Order::getCashfreeFee);
        BigDecimal cashreeTax = sumOrders(pendingOrders, Order::getCashfreeTax);
        BigDecimal platformCharges = sumOrders(pendingOrders, Order::getPlatformFeeAmount);
        BigDecimal refunds = sumOrders(pendingOrders, Order::getRefundAmount);
        BigDecimal pendingAmount = sumOrders(pendingOrders, Order::getVendorNetAmount);

        // Pending from = earliest pending order date
        LocalDate pendingFrom = pendingOrders.stream()
                .map(Order::getCreatedAt)
                .min(Instant::compareTo)
                .map(i -> i.atZone(ZoneId.systemDefault()).toLocalDate())
                .orElse(null);

        return PendingSettlementResponse.builder()
                .pendingFrom(pendingFrom)
                .pendingOrderCount(pendingOrders.size())
                .grossSales(grossSales)
                .cashfreeSales(cashfreeSales)
                .offlineSales(offlineSales)
                .cashreeFees(cashreeFees)
                .cashreeTax(cashreeTax)
                .platformCharges(platformCharges)
                .refunds(refunds)
                .pendingAmount(pendingAmount)
                .pendingTransactions(pendingOrders.stream().map(this::toLedgerEntry).collect(Collectors.toList()))
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
        if (order.getPaymentSource() != null) {
            throw new BadRequestException(
                    "A payment already exists for order " + orderId +
                    ". Payment source: " + order.getPaymentSource());
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

        // 6. Update Order with payment information directly (no separate transaction entity)
        order.setPaymentSource(source);
        order.setPlatformFeePercentage(platformFeePercentage);
        order.setPlatformFeeAmount(platformFeeAmount);
        order.setCashfreeFee(cashfreeFee);
        order.setCashfreeTax(cashfreeTax);
        order.setRefundAmount(refundAmount);
        order.setVendorNetAmount(vendorNetAmount);
        order.setSettlementStatus(SettlementStatus.PENDING);
        
        // Add audit information via metadata
        order.getMetadata().put("recordedBy", staffId);
        order.getMetadata().put("recordedByName", staffName);
        if (request.getNotes() != null) {
            order.getMetadata().put("paymentNotes", request.getNotes());
        }
        if (request.getReference() != null) {
            order.getMetadata().put("paymentReference", request.getReference());
        }
        order.getMetadata().put("recordedAt", Instant.now().toString());
        
        orderRepository.save(order);
        
        log.info("Recorded offline payment for order {} by staff {}: source={}, amount={}",
                orderId, staffId, source, amount);

        return toLedgerEntry(order);
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

    private TransactionLedgerEntry toLedgerEntry(Order order) {
        // Look up settlement ref from settlement id if available
        String settlementRef = null;
        if (order.getSettlementId() != null) {
            settlementRef = settlementRepository.findById(order.getSettlementId())
                    .map(VendorSettlement::getSettlementRef)
                    .orElse(null);
        }

        // Extract audit info from metadata
        String recordedBy = order.getMetadata().get("recordedBy");
        String recordedByName = order.getMetadata().get("recordedByName");
        String notes = order.getMetadata().get("paymentNotes");
        String reference = order.getMetadata().get("paymentReference");
        String recordedAtStr = order.getMetadata().get("recordedAt");
        Instant recordedAt = recordedAtStr != null ? Instant.parse(recordedAtStr) : order.getCreatedAt();

        return TransactionLedgerEntry.builder()
                .transactionId(order.getId()) // Order ID serves as transaction ID
                .orderId(order.getId())
                .paymentId(generateOfflinePaymentId(order.getId()))
                .orderDate(order.getCreatedAt())
                .queueNumber(order.getQueueNumber())
                .paymentSource(order.getPaymentSource())
                .paymentStatus(order.getStatus() == OrderStatus.COMPLETED ? PaymentStatus.COMPLETED : PaymentStatus.PENDING)
                .orderAmount(order.getTotalAmount())
                .cashfreeFee(order.getCashfreeFee())
                .cashfreeTax(order.getCashfreeTax())
                .platformFeePercentage(order.getPlatformFeePercentage())
                .platformFeeAmount(order.getPlatformFeeAmount())
                .refundAmount(order.getRefundAmount())
                .vendorNetAmount(order.getVendorNetAmount())
                .settlementStatus(order.getSettlementStatus())
                .settlementId(order.getSettlementId())
                .settlementRef(settlementRef)
                .settledAt(order.getSettledAt())
                .recordedBy(recordedBy)
                .recordedByName(recordedByName)
                .recordedAt(recordedAt)
                .notes(notes)
                .reference(reference)
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

    /**
     * Sums a BigDecimal field across a list of Order entities, treating null values as ZERO.
     */
    private BigDecimal sumOrders(List<Order> list, java.util.function.Function<Order, BigDecimal> extractor) {
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
