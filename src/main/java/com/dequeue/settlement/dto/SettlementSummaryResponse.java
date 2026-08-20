package com.dequeue.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Top-level financial dashboard cards for ROLE_VENDOR_ADMIN.
 * Contains the summary metrics and "Settled Till" information.
 */
@Data
@Builder
public class SettlementSummaryResponse {

    // ── Revenue cards ─────────────────────────────────────────────────────────

    private BigDecimal totalSales;
    private BigDecimal cashfreeSales;
    private BigDecimal cashSales;
    private BigDecimal offlineSales;

    // ── Deduction cards ───────────────────────────────────────────────────────

    private BigDecimal totalCashfreeFees;
    private BigDecimal totalCashfreeTax;
    private BigDecimal totalPlatformCharges;
    private BigDecimal totalRefunds;

    // ── Earnings ──────────────────────────────────────────────────────────────

    /** Total vendor net earnings = totalSales - all deductions */
    private BigDecimal totalVendorEarnings;

    /** Amount already paid out to the vendor across all settlements. */
    private BigDecimal alreadySettled;

    /** Amount still pending settlement. */
    private BigDecimal pendingSettlement;

    // ── Settled till info ─────────────────────────────────────────────────────

    /**
     * The end-date of the most recent SETTLED settlement.
     * Null if the vendor has never been settled.
     */
    private LocalDate settledTillDate;

    /** Reference ID of the last SETTLED settlement (e.g., "SET-2026-0087"). */
    private String lastSettlementRef;

    /** Net amount of the last SETTLED settlement. */
    private BigDecimal lastSettlementAmount;

    /** Date when the last settlement was actually paid out. */
    private Instant lastSettlementDate;

    /**
     * The start of the pending period — one day after settledTillDate,
     * or the date of the first eligible order if never settled.
     */
    private LocalDate pendingFrom;

    // ── Order counts ──────────────────────────────────────────────────────────

    private int totalOrders;
    private int settledOrders;
    private int pendingOrders;
}
