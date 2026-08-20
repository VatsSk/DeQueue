package com.dequeue.settlement.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.settlement.dto.*;
import com.dequeue.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST API for ROLE_VENDOR_ADMIN Settlement & Financial Reports.
 *
 * <p>All endpoints are:
 * <ul>
 *   <li>Restricted to {@code ROLE_VENDOR_ADMIN}</li>
 *   <li>Scoped to the authenticated vendor via {@code SecurityUtils.getCurrentVendorId()}</li>
 *   <li>A vendor admin can NEVER retrieve another vendor's data by modifying a vendorId param</li>
 * </ul>
 *
 * <p>API map:
 * <pre>
 * GET  /api/v1/vendor/settlements/summary               — dashboard cards
 * GET  /api/v1/vendor/settlements                       — settlement history
 * GET  /api/v1/vendor/settlements/{settlementId}        — settlement detail
 * GET  /api/v1/vendor/settlements/pending               — pending settlement
 * GET  /api/v1/vendor/transactions                      — transaction ledger
 * GET  /api/v1/vendor/transactions/{transactionId}      — single transaction
 * GET  /api/v1/vendor/financial-report                  — full financial report
 * GET  /api/v1/vendor/financial-report/export           — CSV export
 * POST /api/v1/vendor/orders/{orderId}/offline-payment  — record offline payment
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_VENDOR_ADMIN')")
@Tag(name = "Vendor Settlement & Financial Reports",
        description = "Complete financial ledger and settlement management for ROLE_VENDOR_ADMIN")
public class VendorSettlementController {

    private final SettlementService settlementService;

    // ══════════════════════════════════════════════════════════════════════════
    // Settlement Summary Dashboard
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/v1/vendor/settlements/summary")
    @Operation(summary = "Financial dashboard — top-level summary cards",
            description = "Returns total sales, Cashfree sales, offline sales, all fee breakdowns, " +
                    "vendor earnings, already settled, pending settlement, and 'settled till' info.")
    public ApiResponse<SettlementSummaryResponse> getSummary() {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getSummary(vendorId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Settlement History
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/v1/vendor/settlements")
    @Operation(summary = "Settlement history list",
            description = "Paginated list of all settlement batches for this vendor.")
    public ApiResponse<PageResponse<SettlementResponse>> getSettlementHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getSettlementHistory(vendorId, page, size));
    }

    @GetMapping("/api/v1/vendor/settlements/{settlementId}")
    @Operation(summary = "Settlement detail",
            description = "Full detail of a single settlement batch including every included transaction.")
    public ApiResponse<SettlementDetailResponse> getSettlementDetail(
            @PathVariable String settlementId) {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getSettlementDetail(vendorId, settlementId));
    }

    @GetMapping("/api/v1/vendor/settlements/pending")
    @Operation(summary = "Pending settlement",
            description = "Shows all unsettled transactions and the total pending vendor payable.")
    public ApiResponse<PendingSettlementResponse> getPendingSettlement() {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getPendingSettlement(vendorId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Transaction Ledger
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/v1/vendor/transactions")
    @Operation(summary = "Complete transaction ledger",
            description = "Paginated ledger of every payment transaction (online + offline). " +
                    "Supports optional date range filter. Every rupee is traceable to an order.")
    public ApiResponse<PageResponse<TransactionLedgerEntry>> getTransactionLedger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getTransactionLedger(vendorId, from, to, page, size));
    }

    @GetMapping("/api/v1/vendor/transactions/{transactionId}")
    @Operation(summary = "Single transaction detail",
            description = "Full detail of one payment transaction.")
    public ApiResponse<TransactionLedgerEntry> getTransaction(@PathVariable String transactionId) {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getTransaction(vendorId, transactionId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Financial Report
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/v1/vendor/financial-report")
    @Operation(summary = "Full financial report for a date range",
            description = "Complete financial report: total sales, Cashfree/offline breakdown, " +
                    "Cashfree fees, Cashfree taxes, platform charges, refunds, vendor net payable, " +
                    "settlement status, and reconciliation. Cashfree fees are NEVER applied to offline orders.")
    public ApiResponse<FinancialReportResponse> getFinancialReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String vendorId = requireVendorId();
        return ApiResponse.success(settlementService.getFinancialReport(vendorId, from, to));
    }

    @GetMapping(value = "/api/v1/vendor/financial-report/export", produces = "text/csv")
    @Operation(summary = "Export financial report as CSV",
            description = "Downloads the financial report for the given date range as a CSV file. " +
                    "Columns: Order ID, Date, Payment Source, Order Amount, Cashfree Amount, " +
                    "Cashfree Fee, Cashfree Tax, Platform Fee %, Platform Fee Amount, Refund, " +
                    "Vendor Net Amount, Settlement Status, Settlement ID, Settled Date.")
    public ResponseEntity<String> exportFinancialReportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String vendorId = requireVendorId();
        String csv = settlementService.exportFinancialReportCsv(vendorId, from, to);
        String filename = "financial-report-" + from + "-to-" + to + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(csv);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Offline Payment Recording
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/api/v1/vendor/orders/{orderId}/offline-payment")
    @Operation(summary = "Record an offline/cash payment for an order",
            description = "Marks a COMPLETED order as paid offline (cash, bank transfer, etc.). " +
                    "Requires ROLE_VENDOR_ADMIN. Prevents duplicate payments. " +
                    "Stores who recorded the payment and when (full audit trail). " +
                    "Cashfree fees are NOT charged for offline payments. " +
                    "Fee snapshot is taken at time of recording and is immutable.")
    public ApiResponse<TransactionLedgerEntry> recordOfflinePayment(
            @PathVariable String orderId,
            @Valid @RequestBody OfflinePaymentRequest request) {
        String vendorId = requireVendorId();
        String staffId = SecurityUtils.getCurrentUserId();
        String staffName = SecurityUtils.getCurrentUserName();
        return ApiResponse.success(
                settlementService.recordOfflinePayment(vendorId, orderId, request, staffId, staffName),
                "Offline payment recorded successfully");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Derives the vendor ID from the authenticated session.
     * A vendor admin can never access another vendor's data.
     */
    private String requireVendorId() {
        String vendorId = SecurityUtils.getCurrentVendorId();
        if (vendorId == null || vendorId.isBlank()) {
            throw new com.dequeue.common.exception.UnauthorizedException(
                    "No vendor context found for authenticated user");
        }
        return vendorId;
    }
}
