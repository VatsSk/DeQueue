package com.dequeue.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Reconciliation section that shows the vendor numbers must add up.
 *
 * <pre>
 * Eligible Vendor Payable     ₹8,00,000
 * Already Settled            -₹6,00,000
 * ──────────────────────────────────────
 * Pending Settlement           ₹2,00,000
 * </pre>
 */
@Data
@Builder
public class ReconciliationResponse {

    private int totalEligibleOrders;
    private BigDecimal totalEligibleRevenue;

    private BigDecimal totalCashfreeFees;
    private BigDecimal totalCashfreeTax;
    private BigDecimal totalPlatformCharges;
    private BigDecimal totalRefunds;

    /** Total vendor net payable = eligibleRevenue - all deductions. */
    private BigDecimal totalVendorNetPayable;

    /** Already settled across all past settlements. */
    private BigDecimal alreadySettled;

    /**
     * Remaining = totalVendorNetPayable - alreadySettled.
     * This MUST equal pendingSettlement in SettlementSummaryResponse.
     */
    private BigDecimal remainingPendingSettlement;
}
