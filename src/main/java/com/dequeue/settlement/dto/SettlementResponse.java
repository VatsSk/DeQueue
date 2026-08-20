package com.dequeue.settlement.dto;

import com.dequeue.settlement.entity.SettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Settlement list row — shown in the settlement history table.
 */
@Data
@Builder
public class SettlementResponse {

    private String id;
    private String settlementRef;

    private LocalDate periodFrom;
    private LocalDate periodTo;

    private int orderCount;

    // Revenue
    private BigDecimal cashfreeSales;
    private BigDecimal offlineSales;
    private BigDecimal totalSales;

    // Deductions
    private BigDecimal totalCashfreeFees;
    private BigDecimal totalCashfreeTax;
    private BigDecimal totalPlatformCharges;
    private BigDecimal totalRefunds;

    // Net
    private BigDecimal netSettlementAmount;

    private SettlementStatus settlementStatus;
    private Instant settledAt;
    private Instant createdAt;
}
