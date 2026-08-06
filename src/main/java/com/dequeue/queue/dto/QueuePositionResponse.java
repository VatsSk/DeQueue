package com.dequeue.queue.dto;

import com.dequeue.order.entity.OrderStatus;
import lombok.Data;

@Data
public class QueuePositionResponse {
    private String queueNumber;
    private int position;
    private int estimatedWaitTime;
    private OrderStatus status;
}
