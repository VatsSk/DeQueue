package com.dequeue.dashboard.service;

import com.dequeue.dashboard.dto.DashboardResponse;
import com.dequeue.dashboard.dto.TodayStats;
import com.dequeue.order.dto.OrderSummary;

import java.util.List;

public interface DashboardService {
    DashboardResponse getDashboardData(String vendorId);
    TodayStats getTodayStats(String vendorId);
    List<OrderSummary> getRecentOrders(String vendorId);
}
