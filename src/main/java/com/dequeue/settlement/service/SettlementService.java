package com.dequeue.settlement.service;

import com.dequeue.settlement.dto.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface SettlementService {

    /**
     * Returns the top-level financial dashboard summary for the vendor.
     * Covers all time (no date filter) to give the vendor a complete picture.
     */
    SettlementSummaryResponse getSummary(String vendorId);

    /**
     * Returns the paginated settlement history list.
     */
    com.dequeue.common.dto.PageResponse<SettlementResponse> getSettlementHistory(
            String vendorId, int page, int size);

    /**
     * Returns the full detail of a specific settlement, including all included transactions.
     */
    SettlementDetailResponse getSettlementDetail(String vendorId, String settlementId);

    /**
     * Returns the paginated transaction ledger for the vendor, optionally filtered by date.
     */
    com.dequeue.common.dto.PageResponse<TransactionLedgerEntry> getTransactionLedger(
            String vendorId, LocalDate from, LocalDate to, int page, int size);

    /**
     * Returns a single transaction by ID (vendor-scoped).
     */
    TransactionLedgerEntry getTransaction(String vendorId, String transactionId);

    /**
     * Returns the complete financial report for the specified date range.
     */
    FinancialReportResponse getFinancialReport(String vendorId, LocalDate from, LocalDate to);

    /**
     * Returns details of all pending (unsettled) transactions with the pending amount.
     */
    PendingSettlementResponse getPendingSettlement(String vendorId);

    /**
     * Records an offline/cash payment for an order.
     * Enforces: order must be COMPLETED, not already paid, no duplicate transactions.
     *
     * @param vendorId   the authenticated vendor
     * @param orderId    the order to record payment for
     * @param request    the offline payment details
     * @param staffId    authenticated staff ID (from SecurityUtils)
     * @param staffName  authenticated staff name
     * @return the created PaymentTransaction as a TransactionLedgerEntry
     */
    TransactionLedgerEntry recordOfflinePayment(
            String vendorId, String orderId, OfflinePaymentRequest request,
            String staffId, String staffName);

    /**
     * Exports the financial report for the given date range as CSV.
     */
    String exportFinancialReportCsv(String vendorId, LocalDate from, LocalDate to);
}
