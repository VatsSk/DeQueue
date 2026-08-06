package com.dequeue.common.event;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String vendorId;
    private String orderId;
    private String orderStatus;
    private String queueNumber;
    private Instant timestamp;
    private EventType eventType;
    
    public enum EventType {
        ORDER_PLACED, STATUS_CHANGED, ORDER_CANCELLED
    }
}
