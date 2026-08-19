package com.dequeue.order.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class
Order {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    private String vendorCode;
    private String sessionId;
    
    @Builder.Default
    private java.util.Map<String, String> metadata = new java.util.HashMap<>();
    
    private String queueNumber;
    
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    private String customOrderText;
    private BigDecimal totalAmount;
    
    @Indexed
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    private String customerNote;
    
    @Builder.Default
    private List<StatusChange> statusHistory = new ArrayList<>();
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    private Instant completedAt;
    private Instant estimatedReadyTime;
    private Instant preparationStartedAt;
}
