package com.dequeue.order.dto;

import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.entity.StatusChange;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderResponse {
    private String id;
    private String vendorCode;
    private String queueNumber;
    private List<OrderItemResponse> items;
    private String customOrderText;
    private BigDecimal totalAmount;
    
    private BigDecimal subtotal;
    private String couponCode;
    private BigDecimal couponDiscount;
    private String taxName;
    private BigDecimal taxAmount;
    private String serviceChargeName;
    private BigDecimal serviceChargeAmount;
    
    private OrderStatus status;
    private String customerNote;
    private List<StatusChange> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant estimatedReadyTime;
    private String sessionId;
    private String customerSessionToken;
    private java.util.Map<String, String> metadata;
    private Integer rating;
    private String feedback;
}

