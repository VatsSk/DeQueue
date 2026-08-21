package com.dequeue.cashfree.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Platform-level Cashfree and commission configuration.
 * Stored in MongoDB so it can be updated via admin UI without redeploying.
 * Only one document should exist (singleton pattern — id = "global").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cashfree_platform_config")
public class CashfreePlatformConfig {

    @Id
    private String id; // always "global"

    // ── Platform Commission ───────────────────────────────────────────────────

    /** Default commission type applied to all vendors. */
    @Builder.Default
    private CommissionType commissionType = CommissionType.PERCENTAGE;

    /**
     * Default commission rate.
     * If type = PERCENTAGE: this is a percentage (e.g., 5.00 means 5%).
     * If type = FIXED: this is a fixed INR amount per order.
     */
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("5.00");

    // ── Cashfree Integration Status ───────────────────────────────────────────

    /** Whether Cashfree integration is active. */
    @Builder.Default
    private boolean cashfreeEnabled = false;

    /** Whether Cashfree Easy Split is active on this account. */
    @Builder.Default
    private boolean easySplitEnabled = false;

    /** Whether Cashfree webhook is configured. */
    @Builder.Default
    private boolean webhookConfigured = false;

    /** Cashfree environment: sandbox or production. */
    @Builder.Default
    private String environment = "sandbox";

    // ── Audit ─────────────────────────────────────────────────────────────────

    private String updatedBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
