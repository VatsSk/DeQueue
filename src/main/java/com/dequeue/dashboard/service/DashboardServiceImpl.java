package com.dequeue.dashboard.service;

import com.dequeue.dashboard.dto.DashboardResponse;
import com.dequeue.dashboard.dto.TodayStats;
import com.dequeue.order.dto.OrderSummary;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.vendor.repository.VendorRepository;
import com.dequeue.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final OrderMapper orderMapper;

    @Override
    public DashboardResponse getDashboardData(String vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId).orElseThrow();
        DashboardResponse response = new DashboardResponse();
        response.setShopStatus(vendor.getShopStatus());
        response.setTodayStats(getTodayStats(vendorId));
        
        List<OrderSummary> recent = getRecentOrders(vendorId);
        response.setRecentOrders(recent);
        
        // Calculate average wait time logic: for completed orders today
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Order> todayOrders = orderRepository.findByVendorIdAndCreatedAtAfter(vendorId, startOfDay);
        long totalMins = 0;
        int completedCount = 0;
        for (Order o : todayOrders) {
            if (o.getStatus() == OrderStatus.COMPLETED && o.getCompletedAt() != null) {
                totalMins += Duration.between(o.getCreatedAt(), o.getCompletedAt()).toMinutes();
                completedCount++;
            }
        }
        
        int avgWait = completedCount > 0 ? (int)(totalMins / completedCount) : 0;
        response.setAverageWaitTime(avgWait);
        
        return response;
    }
    
    @Override
    public TodayStats getTodayStats(String vendorId) {
        TodayStats stats = new TodayStats();
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Order> todayOrders = orderRepository.findByVendorIdAndCreatedAtAfter(vendorId, startOfDay);
        
        stats.setTotalOrders(todayOrders.size());
        
        int pending = 0;
        int preparing = 0;
        int ready = 0;
        int completed = 0;
        int cancelled = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        
        for (Order o : todayOrders) {
            if (o.getStatus() == OrderStatus.PENDING) pending++;
            else if (o.getStatus() == OrderStatus.PREPARING) preparing++;
            else if (o.getStatus() == OrderStatus.READY) ready++;
            else if (o.getStatus() == OrderStatus.COMPLETED) completed++;
            else if (o.getStatus() == OrderStatus.CANCELLED) cancelled++;
            
            if (o.getStatus() == OrderStatus.COMPLETED) {
                revenue = revenue.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
            }
        }
        
        stats.setPendingOrders(pending);
        stats.setPreparingOrders(preparing);
        stats.setReadyOrders(ready);
        stats.setCollectedOrders(completed);
        stats.setCancelledOrders(cancelled);
        stats.setTotalRevenue(revenue);
        return stats;
    }
    
    @Override
    public List<OrderSummary> getRecentOrders(String vendorId) {
        List<Order> activeOrders = orderRepository.findByVendorIdAndStatusIn(
            vendorId, 
            List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY)
        );
        activeOrders.sort(Comparator.comparing(Order::getCreatedAt).reversed());
        return orderMapper.toSummaryList(activeOrders);
    }
}
