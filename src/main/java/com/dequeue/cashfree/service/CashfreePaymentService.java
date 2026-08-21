package com.dequeue.cashfree.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstraction for Cashfree Payment Gateway operations.
 * All Cashfree HTTP calls go through this service — never scattered in controllers.
 */
public interface CashfreePaymentService {

    /**
     * Create a Cashfree payment order.
     *
     * @param orderId      DeQueue order ID (used as Cashfree order_id).
     * @param amount       Amount in INR.
     * @param customerName Customer name.
     * @param customerEmail Customer email.
     * @param customerPhone Customer phone.
     * @param returnUrl    URL to redirect after payment.
     * @return             Cashfree response including payment_session_id.
     */
    Map<String, Object> createPaymentOrder(
            String orderId, BigDecimal amount,
            String customerName, String customerEmail, String customerPhone,
            String returnUrl);

    /**
     * Get payment status for a Cashfree order.
     *
     * @param cashfreeOrderId  Cashfree order ID.
     * @return                 Map with payment status details.
     */
    Map<String, Object> getPaymentStatus(String cashfreeOrderId);

    /**
     * Get all payments for a Cashfree order.
     *
     * @param cashfreeOrderId  Cashfree order ID.
     * @return                 List of payment attempts.
     */
    Object getPaymentsForOrder(String cashfreeOrderId);

    /**
     * Initiate a refund for a payment.
     *
     * @param cashfreeOrderId  Cashfree order ID.
     * @param refundId         Unique refund reference.
     * @param amount           Refund amount.
     * @param note             Refund reason.
     * @return                 Cashfree refund response.
     */
    Map<String, Object> createRefund(
            String cashfreeOrderId, String refundId, BigDecimal amount, String note);
}
