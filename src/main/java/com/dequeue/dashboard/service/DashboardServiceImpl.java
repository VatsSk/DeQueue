package com.dequeue.dashboard.service;

import com.dequeue.dashboard.dto.DashboardResponse;
import com.dequeue.dashboard.dto.TodayStats;
import com.dequeue.order.dto.OrderSummary;
import com.dequeue.vendor.entity.ShopStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    @Override
    public DashboardResponse getDashboardData(String vendorId) {
        DashboardResponse response = new DashboardResponse();
        response.setShopStatus(ShopStatus.OPEN);
        response.setTodayStats(new TodayStats());
        response.setRecentOrders(new ArrayList<>());
        return response;
    }
    
    @Override
    public TodayStats getTodayStats(String vendorId) {
        TodayStats stats = new TodayStats();
        stats.setTotalRevenue(BigDecimal.ZERO);
        return stats;
    }
    
    @Override
    public List<OrderSummary> getRecentOrders(String vendorId) {
        return new ArrayList<>();
    }
}
