package com.dequeue.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Offline (Cash / bank transfer) payment breakdown section of the financial report.
 * Cashfree fees are always ₹0 for offline payments.
 */
@Data
@Builder
public class OfflinePaymentSummary {

    private int cashOrderCount;
    private int offlineOrderCount;
    private BigDecimal cashAmount;
    private BigDecimal offlineAmount;
    private BigDecimal grossAmount;

    /** Always ₹0 — Cashfree does not charge fees on offline transactions. */
    private BigDecimal cashfreeFees;

    private BigDecimal platformFees;
    private BigDecimal refunds;
    private BigDecimal vendorNetAmount;
}
