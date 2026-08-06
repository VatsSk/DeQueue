package com.dequeue.queue.dto;

import com.dequeue.order.entity.OrderStatus;
import lombok.Data;
import java.time.Instant;

@Data
public class QueueItem {
    private String queueNumber;
    private OrderStatus status;
    private int itemCount;
    private Instant createdAt;
    private Instant estimatedReadyTime;
}

