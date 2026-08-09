package com.dequeue.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusEvent {
    private String eventId;          // Unique UUID for deduplication
    private String orderId;
    private String vendorId;
    private String queueNumber;
    private String sessionId;
    private String status;           // OrderStatus name
    private String message;          // Human-readable status message
    private Instant timestamp;
}
