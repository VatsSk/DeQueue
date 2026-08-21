package com.dequeue.order.dto;

import com.dequeue.settlement.entity.PaymentSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class PlaceOrderRequest {
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
    private String customerNote;
    private String customOrderText;
    private Double customerLatitude;
    private Double customerLongitude;
    private String sessionId;
    private java.util.Map<String, String> metadata;
    private java.util.Map<String, String> customFields;
    
    // Payment source - CASH, CASHFREE, OFFLINE, OTHER
    private PaymentSource paymentSource;
    
    // new fields
    private java.math.BigDecimal subtotal;
    private String couponCode;
    private java.math.BigDecimal couponDiscount;
    private String taxName;
    private java.math.BigDecimal taxAmount;
    private String serviceChargeName;
    private java.math.BigDecimal serviceChargeAmount;
}
