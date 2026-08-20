package com.dequeue.settlement.dto;

import com.dequeue.settlement.entity.SettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full detail for a single VendorSettlement — shown when the vendor clicks a
 * row in the settlement history table. Includes every included transaction.
 */
@Data
@Builder
public class SettlementDetailResponse {

    private String id;
    private String settlementRef;

    // ── Period ────────────────────────────────────────────────────────────────

    private LocalDate periodFrom;
    private LocalDate periodTo;

    // ── Order summary ─────────────────────────────────────────────────────────

    private int orderCount;

    // ── Revenue breakdown ─────────────────────────────────────────────────────

    private BigDecimal cashfreeSales;
    private BigDecimal offlineSales;
    private BigDecimal totalSales;

    // ── Deductions ────────────────────────────────────────────────────────────

    private BigDecimal totalCashfreeFees;
    private BigDecimal totalCashfreeTax;
    private BigDecimal totalPlatformCharges;
    private BigDecimal totalRefunds;

    // ── Net ───────────────────────────────────────────────────────────────────

    private BigDecimal netSettlementAmount;

    // ── Status ────────────────────────────────────────────────────────────────

    private SettlementStatus settlementStatus;
    private Instant settledAt;
    private Instant createdAt;
    private String adminNotes;

    // ── Included transactions ─────────────────────────────────────────────────

    /** Full list of every transaction included in this settlement batch. */
    private List<TransactionLedgerEntry> transactions;
}
