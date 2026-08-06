package com.dequeue.reports.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class HourlyData {
    private int hour;
    private int orderCount;
    private BigDecimal revenue;
}
