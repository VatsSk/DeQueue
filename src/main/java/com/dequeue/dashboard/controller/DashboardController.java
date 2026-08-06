package com.dequeue.dashboard.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.dashboard.dto.DashboardResponse;
import com.dequeue.dashboard.dto.TodayStats;
import com.dequeue.dashboard.service.DashboardService;
import com.dequeue.order.dto.OrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard() {
        return ApiResponse.success(dashboardService.getDashboardData(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/stats")
    public ApiResponse<TodayStats> getStats() {
        return ApiResponse.success(dashboardService.getTodayStats(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/recent-orders")
    public ApiResponse<List<OrderSummary>> getRecentOrders() {
        return ApiResponse.success(dashboardService.getRecentOrders(SecurityUtils.getCurrentVendorId()));
    }
}
