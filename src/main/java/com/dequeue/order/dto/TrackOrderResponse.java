package com.dequeue.order.dto;

import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.entity.StatusChange;
import com.dequeue.settlement.entity.PaymentSource;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class TrackOrderResponse {
    private String queueNumber;
    private OrderStatus status;
    private int positionInQueue;
    private int estimatedWaitTime;
    private List<StatusChange> statusHistory;
    private Instant createdAt;
    
    // Payment information for customer display
    private PaymentSource paymentSource;
    private BigDecimal totalAmount;
    
    private Map<String, String> customFields;
}
