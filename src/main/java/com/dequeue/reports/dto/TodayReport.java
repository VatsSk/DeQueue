package com.dequeue.reports.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class TodayReport {
    private LocalDate date;
    private int totalOrders;
    private int completedOrders;
    private int pendingOrders;
    private int cancelledOrders;
    private int averagePrepTime;
    private BigDecimal totalRevenue;
    private Map<String, Double> comparedToYesterday;
}
