package com.dequeue.cashfree.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentOrderResponse {
    private String dequeueOrderId;
    private String cashfreeOrderId;
    private String paymentSessionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String environment; // sandbox or production
}
