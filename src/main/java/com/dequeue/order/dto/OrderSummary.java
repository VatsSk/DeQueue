package com.dequeue.order.dto;

import com.dequeue.order.entity.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class OrderSummary {
    private String id;
    private String queueNumber;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private int itemCount;
    private Instant createdAt;
}

