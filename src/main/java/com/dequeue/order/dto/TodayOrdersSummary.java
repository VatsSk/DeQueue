package com.dequeue.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TodayOrdersSummary {
    private int totalOrders;
    private int pendingCount;
    private int preparingCount;
    private int readyCount;
    private int collectedCount;
    private int cancelledCount;
    private BigDecimal totalRevenue;
}
