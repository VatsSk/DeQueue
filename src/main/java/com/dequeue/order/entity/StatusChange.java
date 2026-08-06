package com.dequeue.order.entity;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChange {
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String changedBy;
    private String changedByName;
    private Instant changedAt;
    private String note;
}
