package com.dequeue.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Online (Cashfree) payment breakdown section of the financial report.
 */
@Data
@Builder
public class OnlinePaymentSummary {

    private int orderCount;
    private BigDecimal grossAmount;
    private BigDecimal cashfreeFees;
    private BigDecimal cashfreeTax;
    private BigDecimal platformFees;
    private BigDecimal refunds;
    private BigDecimal vendorNetAmount;
}
