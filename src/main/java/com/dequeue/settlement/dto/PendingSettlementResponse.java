package com.dequeue.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Shows the vendor exactly which orders are still pending settlement and why.
 */
@Data
@Builder
public class PendingSettlementResponse {

    /** From which date the pending amount accrues. */
    private LocalDate pendingFrom;

    private int pendingOrderCount;

    // Revenue
    private BigDecimal grossSales;
    private BigDecimal cashfreeSales;
    private BigDecimal offlineSales;

    // Deductions
    private BigDecimal cashreeFees;
    private BigDecimal cashreeTax;
    private BigDecimal platformCharges;
    private BigDecimal refunds;

    /** The net amount still owed to the vendor. */
    private BigDecimal pendingAmount;

    /** The individual pending transactions. */
    private List<TransactionLedgerEntry> pendingTransactions;
}
