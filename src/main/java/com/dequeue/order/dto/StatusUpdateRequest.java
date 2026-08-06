package com.dequeue.order.dto;

import com.dequeue.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull
    private OrderStatus status;
    private String note;
}
