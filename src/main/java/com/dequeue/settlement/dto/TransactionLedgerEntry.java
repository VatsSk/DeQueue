package com.dequeue.settlement.dto;

import com.dequeue.settlement.entity.PaymentSource;
import com.dequeue.settlement.entity.PaymentStatus;
import com.dequeue.settlement.entity.SettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single row in the vendor's transaction ledger.
 * Every field must be traceable back to an order and payment transaction.
 */
@Data
@Builder
public class TransactionLedgerEntry {

    // ── Identity ──────────────────────────────────────────────────────────────

    private String transactionId;
    private String orderId;
    private String paymentId;
    private Instant orderDate;
    private String queueNumber;

    // ── Payment ───────────────────────────────────────────────────────────────

    private PaymentSource paymentSource;
    private PaymentStatus paymentStatus;
    private BigDecimal orderAmount;

    // ── Fee breakdown ─────────────────────────────────────────────────────────

    /** Cashfree gateway fee (₹0 for cash/offline). */
    private BigDecimal cashfreeFee;

    /** Cashfree tax on gateway fee (₹0 for cash/offline). */
    private BigDecimal cashfreeTax;

    /** Platform commission percentage snapshot. */
    private BigDecimal platformFeePercentage;

    /** Platform commission amount snapshot. */
    private BigDecimal platformFeeAmount;

    /** Refund amount (₹0 if no refund). */
    private BigDecimal refundAmount;

    /** Net amount due to vendor for this order. */
    private BigDecimal vendorNetAmount;

    // ── Settlement tracking ───────────────────────────────────────────────────

    private SettlementStatus settlementStatus;
    private String settlementId;
    private String settlementRef;
    private Instant settledAt;

    // ── Audit ─────────────────────────────────────────────────────────────────

    /** For offline payments: who recorded the payment. */
    private String recordedBy;
    private String recordedByName;
    private Instant recordedAt;
    private String notes;
    private String reference;
}
