package com.dequeue.order.dto;

import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.entity.StatusChange;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class TrackOrderResponse {
    private String queueNumber;
    private OrderStatus status;
    private int positionInQueue;
    private int estimatedWaitTime;
    private List<StatusChange> statusHistory;
    private Instant createdAt;
}

