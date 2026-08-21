package com.dequeue.cashfree.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstraction for Cashfree Easy Split vendor and split operations.
 */
public interface CashfreeEasySplitService {

    /**
     * Create a vendor in Cashfree Easy Split.
     * Uses deterministic vendor ID: "DEQUEUE_<mongoVendorId>".
     */
    Map<String, Object> createVendor(String mongoVendorId, Map<String, Object> vendorDetails);

    /**
     * Get vendor status from Cashfree.
     */
    Map<String, Object> getVendor(String cashfreeVendorId);

    /**
     * Update vendor details in Cashfree.
     */
    Map<String, Object> updateVendor(String cashfreeVendorId, Map<String, Object> vendorDetails);

    /**
     * Create split after payment — split-after-payment flow.
     * Called after Cashfree confirms payment success via webhook.
     *
     * @param cashfreeOrderId    Cashfree order ID.
     * @param cashfreeVendorId   Cashfree vendor ID.
     * @param vendorAmount       Amount to credit to vendor.
     * @param platformAmount     Amount to retain as platform commission.
     */
    Map<String, Object> createSplitAfterPayment(
            String cashfreeOrderId,
            String cashfreeVendorId,
            BigDecimal vendorAmount,
            BigDecimal platformAmount);

    /**
     * Get split and settlement details for a specific Cashfree order.
     */
    Map<String, Object> getSplitByOrderId(String cashfreeOrderId);

    /**
     * Get vendor settlement details.
     */
    Map<String, Object> getVendorSettlements(String cashfreeVendorId, String from, String to);

    /**
     * Build the deterministic Cashfree vendor ID for a DeQueue vendor.
     */
    static String buildCashfreeVendorId(String mongoVendorId) {
        return "DEQUEUE_" + mongoVendorId;
    }
}
