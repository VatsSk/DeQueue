package com.dequeue.settlement.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Records every payment event for an order — both online (Cashfree) and
 * offline (cash / bank transfer / etc.).
 *
 * <p>Financial integrity: once a transaction is COMPLETED, fee snapshot fields
 * (cashfreeFee, cashfreeTax, platformFeePercentage, platformFeeAmount,
 * vendorNetAmount) must NOT be mutated. Historical records always retain the
 * fee rates that were in effect at the time of finalization.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payment_transactions")
public class PaymentTransaction {

    @Id
    private String id;

    /** The order this transaction belongs to. */
    @Indexed
    private String orderId;

    /** The vendor that owns this order. Derived from the order — never set by callers. */
    @Indexed
    private String vendorId;

    /**
     * External gateway payment ID (e.g., Cashfree's cf_payment_id).
     * For offline payments this is a human-readable reference like "CASH-20260819-001".
     */
    private String paymentId;

    /** How the customer paid. */
    private PaymentSource paymentSource;

    /** Gross amount paid by the customer (equals order totalAmount). */
    private BigDecimal amount;

    /** Current lifecycle state of this payment. */
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ── Fee snapshot (immutable once COMPLETED) ──────────────────────────────

    /**
     * Gateway fee charged by Cashfree for this transaction.
     * Zero for CASH / OFFLINE payments.
     */
    @Builder.Default
    private BigDecimal cashfreeFee = BigDecimal.ZERO;

    /**
     * GST / tax levied by Cashfree on their gateway fee.
     * Zero for CASH / OFFLINE payments.
     */
    @Builder.Default
    private BigDecimal cashfreeTax = BigDecimal.ZERO;

    /**
     * DeQueue platform commission percentage snapshot at time of finalization.
     * Stored so historical records are never affected by future rate changes.
     */
    @Builder.Default
    private BigDecimal platformFeePercentage = BigDecimal.ZERO;

    /**
     * Calculated platform fee amount = amount × platformFeePercentage / 100.
     */
    @Builder.Default
    private BigDecimal platformFeeAmount = BigDecimal.ZERO;

    /** Any refund applied against this transaction. */
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /**
     * Net amount due to the vendor for this transaction:
     *   amount − cashfreeFee − cashfreeTax − platformFeeAmount − refundAmount
     */
    @Builder.Default
    private BigDecimal vendorNetAmount = BigDecimal.ZERO;

    // ── Settlement tracking ───────────────────────────────────────────────────

    /** Which VendorSettlement batch this transaction was included in (null = pending). */
    private String settlementId;

    /** Settlement status for this individual transaction. */
    @Builder.Default
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    /** When this transaction was included in a settlement batch. */
    private Instant settledAt;

    // ── Audit ─────────────────────────────────────────────────────────────────

    /**
     * Staff ID who recorded this transaction.
     * For Cashfree: populated by webhook / payment verification flow.
     * For offline: set to the authenticated vendor admin user ID.
     */
    private String recordedBy;

    /** Display name of the staff who recorded this transaction. */
    private String recordedByName;

    /** Timestamp when this record was created. */
    @CreatedDate
    private Instant recordedAt;

    /** Free-text notes (e.g., "Customer paid cash at counter"). */
    private String notes;

    /** External reference (e.g., UPI transaction ID, cheque number). */
    private String reference;

    @LastModifiedDate
    private Instant updatedAt;
}
