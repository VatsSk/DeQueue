package com.dequeue.vendor.entity;

import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHour {
    private DayOfWeek dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean closed;
}
