package com.dequeue.cashfree.controller;

import com.dequeue.cashfree.dto.CommissionConfigRequest;
import com.dequeue.cashfree.dto.PlatformConfigResponse;
import com.dequeue.cashfree.entity.CashfreePlatformConfig;
import com.dequeue.cashfree.repository.CashfreePlatformConfigRepository;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.settlement.entity.PaymentSource;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.entity.VendorSettings;
import com.dequeue.vendor.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("T(com.dequeue.common.security.SecurityUtils).isPlatformAdmin()")
@RequiredArgsConstructor
@Tag(name = "Admin Financial Dashboard", description = "Platform-level financial reporting and configuration")
public class AdminFinancialController {

    private static final String GLOBAL_CONFIG_ID = "global";

    private final CashfreePlatformConfigRepository platformConfigRepository;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;

    // ── Commission Configuration ──────────────────────────────────────────────

    @GetMapping("/payments/config")
    @Operation(summary = "Get platform payment/commission configuration")
    public ApiResponse<PlatformConfigResponse> getConfig() {
        CashfreePlatformConfig config = platformConfigRepository.findById(GLOBAL_CONFIG_ID)
                .orElseGet(this::defaultConfig);
        return ApiResponse.success(toPlatformConfigResponse(config));
    }

    @PostMapping("/payments/config/commission")
    @Operation(summary = "Update global commission configuration")
    public ApiResponse<PlatformConfigResponse> updateCommission(
            @Valid @RequestBody CommissionConfigRequest request) {
        CashfreePlatformConfig config = platformConfigRepository.findById(GLOBAL_CONFIG_ID)
                .orElseGet(this::defaultConfig);
        config.setCommissionType(request.getCommissionType());
        config.setCommissionRate(request.getCommissionRate());
        config.setUpdatedBy(SecurityUtils.getCurrentUserId());
        platformConfigRepository.save(config);
        log.info("Global commission updated: type={}, rate={} by {}",
                request.getCommissionType(), request.getCommissionRate(),
                SecurityUtils.getCurrentUserId());
        return ApiResponse.success(toPlatformConfigResponse(config),
                "Commission configuration updated successfully");
    }

    @PostMapping("/payments/config/vendor/{vendorId}/commission")
    @Operation(summary = "Set vendor-specific commission override")
    public ApiResponse<String> setVendorCommission(
            @PathVariable String vendorId,
            @Valid @RequestBody CommissionConfigRequest request) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));
        if (vendor.getSettings() == null) {
            vendor.setSettings(new VendorSettings());
        }
        vendor.getSettings().setPlatformFeePercentage(request.getCommissionRate());
        vendorRepository.save(vendor);
        log.info("Vendor {} commission override set to {}", vendorId, request.getCommissionRate());
        return ApiResponse.success("Vendor commission updated");
    }

    // ── Financial Summary ─────────────────────────────────────────────────────

    @GetMapping("/financial-summary")
    @Operation(summary = "Platform-wide financial summary")
    public ApiResponse<Map<String, Object>> getFinancialSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Order> orders = fetchOrders(null, from, to);
        return ApiResponse.success(buildSummary(orders));
    }

    @GetMapping("/financial-summary/vendor/{vendorId}")
    @Operation(summary = "Vendor-specific financial summary")
    public ApiResponse<Map<String, Object>> getVendorFinancialSummary(
            @PathVariable String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Order> orders = fetchOrders(vendorId, from, to);
        return ApiResponse.success(buildSummary(orders));
    }

    @GetMapping("/payments")
    @Operation(summary = "List all payments across vendors")
    public ApiResponse<List<Map<String, Object>>> getAllPayments(
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<Order> orders = fetchOrders(vendorId, from, to);
        List<Map<String, Object>> result = orders.stream()
                .filter(o -> o.getPaymentSource() != null)
                .filter(o -> paymentMethod == null || paymentMethod.equalsIgnoreCase(o.getPaymentSource().name()))
                .skip((long) page * size)
                .limit(size)
                .map(this::toPaymentSummary)
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    @GetMapping("/reconciliation")
    @Operation(summary = "Admin reconciliation view")
    public ApiResponse<Map<String, Object>> getReconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<Order> orders = fetchOrders(null, from, to);
        List<Order> paid = orders.stream().filter(o -> o.getPaymentSource() != null).toList();
        List<Order> onlineOrders = paid.stream().filter(o -> o.getPaymentSource() == PaymentSource.CASHFREE).toList();

        long splitMissing = onlineOrders.stream().filter(o -> o.getCashfreeSplitId() == null).count();
        long paymentIdMissing = onlineOrders.stream().filter(o -> o.getCashfreePaymentId() == null).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", orders.size());
        result.put("paidOrders", paid.size());
        result.put("onlineOrders", onlineOrders.size());
        result.put("offlineOrders", paid.size() - onlineOrders.size());
        result.put("splitMissing", splitMissing);
        result.put("cashfreePaymentIdMissing", paymentIdMissing);
        result.put("fromDate", from);
        result.put("toDate", to);

        return ApiResponse.success(result);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private List<Order> fetchOrders(String vendorId, LocalDate from, LocalDate to) {
        if (vendorId != null && from != null && to != null) {
            return orderRepository.findByVendorIdAndStatusAndCreatedAtBetween(
                    vendorId, OrderStatus.COMPLETED, toInstant(from), toInstantEnd(to));
        } else if (vendorId != null) {
            return orderRepository.findByVendorIdAndStatus(vendorId, OrderStatus.COMPLETED);
        } else if (from != null && to != null) {
            // All vendors in date range
            return orderRepository.findAll().stream()
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                    .filter(o -> o.getCreatedAt() != null
                            && !o.getCreatedAt().isBefore(toInstant(from))
                            && !o.getCreatedAt().isAfter(toInstantEnd(to)))
                    .collect(Collectors.toList());
        } else {
            return orderRepository.findAll().stream()
                    .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                    .collect(Collectors.toList());
        }
    }

    private Map<String, Object> buildSummary(List<Order> orders) {
        List<Order> paid = orders.stream().filter(o -> o.getPaymentSource() != null).toList();
        List<Order> online = paid.stream().filter(o -> o.getPaymentSource() == PaymentSource.CASHFREE).toList();
        List<Order> offline = paid.stream().filter(o -> o.getPaymentSource() != PaymentSource.CASHFREE).toList();

        BigDecimal totalGmv = sum(paid, Order::getTotalAmount);
        BigDecimal onlineGmv = sum(online, Order::getTotalAmount);
        BigDecimal offlineGmv = sum(offline, Order::getTotalAmount);
        BigDecimal totalCommission = sum(paid, Order::getPlatformFeeAmount);
        BigDecimal cashfreeFees = sum(online, Order::getCashfreeFee);
        BigDecimal cashfreeTaxes = sum(online, Order::getCashfreeTax);
        BigDecimal platformNetRevenue = totalCommission.subtract(cashfreeFees).subtract(cashfreeTaxes);
        BigDecimal totalVendorAmount = sum(paid, Order::getVendorNetAmount);
        BigDecimal totalRefunds = sum(paid, Order::getRefundAmount);
        BigDecimal offlineCommissionReceivable = sum(offline, Order::getPlatformFeeAmount);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalGmv", totalGmv);
        summary.put("onlineGmv", onlineGmv);
        summary.put("offlineGmv", offlineGmv);
        summary.put("totalOrders", paid.size());
        summary.put("onlineOrders", online.size());
        summary.put("offlineOrders", offline.size());
        summary.put("platformGrossCommission", totalCommission);
        summary.put("cashfreeFees", cashfreeFees);
        summary.put("cashfreeTaxes", cashfreeTaxes);
        summary.put("platformNetRevenue", platformNetRevenue);
        summary.put("totalVendorAmount", totalVendorAmount);
        summary.put("totalRefunds", totalRefunds);
        summary.put("offlineCommissionReceivable", offlineCommissionReceivable);
        return summary;
    }

    private Map<String, Object> toPaymentSummary(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", order.getId());
        m.put("vendorId", order.getVendorId());
        m.put("queueNumber", order.getQueueNumber());
        m.put("orderAmount", order.getTotalAmount());
        m.put("paymentSource", order.getPaymentSource());
        m.put("cashfreeOrderId", order.getCashfreeOrderId());
        m.put("cashfreePaymentId", order.getCashfreePaymentId());
        m.put("cashfreeSplitId", order.getCashfreeSplitId());
        m.put("platformCommission", order.getPlatformFeeAmount());
        m.put("platformCommissionRate", order.getPlatformFeePercentage());
        m.put("cashfreeFee", order.getCashfreeFee());
        m.put("cashfreeTax", order.getCashfreeTax());
        m.put("vendorNetAmount", order.getVendorNetAmount());
        m.put("refundAmount", order.getRefundAmount());
        m.put("settlementStatus", order.getSettlementStatus());
        m.put("createdAt", order.getCreatedAt());
        return m;
    }

    private PlatformConfigResponse toPlatformConfigResponse(CashfreePlatformConfig config) {
        return PlatformConfigResponse.builder()
                .commissionType(config.getCommissionType())
                .commissionRate(config.getCommissionRate())
                .cashfreeEnabled(config.isCashfreeEnabled())
                .easySplitEnabled(config.isEasySplitEnabled())
                .webhookConfigured(config.isWebhookConfigured())
                .environment(config.getEnvironment())
                .build();
    }

    private CashfreePlatformConfig defaultConfig() {
        return CashfreePlatformConfig.builder()
                .id(GLOBAL_CONFIG_ID)
                .commissionType(com.dequeue.cashfree.entity.CommissionType.PERCENTAGE)
                .commissionRate(new BigDecimal("5.00"))
                .environment("sandbox")
                .build();
    }

    private BigDecimal sum(List<Order> orders, java.util.function.Function<Order, BigDecimal> fn) {
        return orders.stream()
                .map(fn)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant toInstantEnd(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
    }
}
