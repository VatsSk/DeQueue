package com.dequeue.settlement.entity;

/**
 * Lifecycle status of a VendorSettlement record and individual order settlement.
 */
public enum SettlementStatus {
    PENDING,
    PROCESSING,
    SETTLED,
    FAILED
}
