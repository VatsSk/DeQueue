package com.dequeue.settlement.entity;

/**
 * Lifecycle status of a PaymentTransaction.
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}
