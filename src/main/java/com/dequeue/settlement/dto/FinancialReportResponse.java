package com.dequeue.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Complete financial report response for a given date range.
 * Returned by GET /api/v1/vendor/settlements/financial-report
 */
@Data
@Builder
public class FinancialReportResponse {

    // ── Report period ─────────────────────────────────────────────────────────

    private LocalDate fromDate;
    private LocalDate toDate;

    // ── Top-level totals ──────────────────────────────────────────────────────

    private BigDecimal totalSales;
    private BigDecimal cashfreeSales;
    private BigDecimal cashSales;
    private BigDecimal offlineSales;

    // ── Deductions (each shown separately — never as a single "Total Fees") ───

    /** Gateway fees charged by Cashfree — applies only to Cashfree transactions. */
    private BigDecimal cashfreeFees;

    /** GST / tax on Cashfree gateway fees — applies only to Cashfree transactions. */
    private BigDecimal cashfreeTax;

    /** DeQueue platform commission — applies to ALL transactions. */
    private BigDecimal platformCharges;

    /** Total refunds in the period. */
    private BigDecimal refunds;

    // ── Net vendor payable ────────────────────────────────────────────────────

    /** totalSales - cashfreeFees - cashfreeTax - platformCharges - refunds */
    private BigDecimal vendorNetPayable;

    // ── Settlement info for the period ───────────────────────────────────────

    private BigDecimal alreadySettled;
    private BigDecimal pendingSettlement;

    // ── Breakdown sections ────────────────────────────────────────────────────

    /** Online (Cashfree) specific breakdown. */
    private OnlinePaymentSummary onlineBreakdown;

    /** Offline (Cash + other) specific breakdown. */
    private OfflinePaymentSummary offlineBreakdown;

    /** Settlement-by-settlement reconciliation. */
    private ReconciliationResponse reconciliation;

    // ── Transaction ledger ────────────────────────────────────────────────────

    private int totalTransactions;
    private List<TransactionLedgerEntry> transactions;
}
