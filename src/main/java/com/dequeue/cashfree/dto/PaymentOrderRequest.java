package com.dequeue.cashfree.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentOrderRequest {
    @NotBlank
    private String orderId; // DeQueue order ID

    @NotNull
    @Positive
    private BigDecimal amount;

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String returnUrl;
}
