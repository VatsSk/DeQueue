package com.dequeue.settlement.entity;

/**
 * Identifies how an order was paid.
 * CASHFREE = online payment via Cashfree gateway.
 * CASH     = physical cash collected at the counter.
 * OFFLINE  = any other offline method (bank transfer, UPI QR, etc.).
 * OTHER    = catch-all for edge cases.
 */
public enum PaymentSource {
    CASHFREE,
    CASH,
    OFFLINE,
    OTHER
}
