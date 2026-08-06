package com.dequeue.dashboard.dto;

import com.dequeue.order.dto.OrderSummary;
import com.dequeue.vendor.entity.ShopStatus;
import lombok.Data;

import java.util.List;

@Data
public class DashboardResponse {
    private ShopStatus shopStatus;
    private String currentlyServing;
    private int queueLength;
    private TodayStats todayStats;
    private List<OrderSummary> recentOrders;
    private int averageWaitTime;
    private String peakHour;
}
