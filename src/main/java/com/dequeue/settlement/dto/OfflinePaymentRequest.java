package com.dequeue.settlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for recording an offline / cash payment for an order.
 * Only ROLE_VENDOR_ADMIN may submit this.
 */
@Data
public class OfflinePaymentRequest {

    @NotBlank(message = "Payment source is required (CASH or OFFLINE)")
    private String paymentSource; // "CASH" or "OFFLINE"

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    /** Optional human-readable reference (e.g., UPI transaction ID, cheque number). */
    private String reference;

    /** Free-text notes. */
    private String notes;
}
