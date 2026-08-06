package com.dequeue.vendor.entity;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private String plan;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
}
