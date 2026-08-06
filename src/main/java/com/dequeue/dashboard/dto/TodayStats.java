package com.dequeue.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TodayStats {
    private int totalOrders;
    private int pendingOrders;
    private int preparingOrders;
    private int readyOrders;
    private int collectedOrders;
    private int cancelledOrders;
    private BigDecimal totalRevenue;
}
