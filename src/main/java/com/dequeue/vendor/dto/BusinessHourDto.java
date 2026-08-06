package com.dequeue.vendor.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class BusinessHourDto {
    private String dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private boolean closed;
}
