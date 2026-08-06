package com.dequeue.order.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.order.dto.OrderResponse;
import com.dequeue.order.dto.OrderSummary;
import com.dequeue.order.dto.StatusUpdateRequest;
import com.dequeue.order.dto.TodayOrdersSummary;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.service.OrderService;
import com.dequeue.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResponse<OrderSummary>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String queueNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.listOrders(SecurityUtils.getCurrentVendorId(), status, startDate, endDate, queueNumber, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable String id) {
        return ApiResponse.success(orderService.getOrder(SecurityUtils.getCurrentVendorId(), id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResponse.success(orderService.updateStatus(SecurityUtils.getCurrentVendorId(), id, request));
    }

    @GetMapping("/active")
    public ApiResponse<List<OrderSummary>> getActiveOrders() {
        return ApiResponse.success(orderService.getActiveOrders(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/today")
    public ApiResponse<TodayOrdersSummary> getTodaySummary() {
        return ApiResponse.success(orderService.getTodaySummary(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<OrderSummary>> getHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.getHistory(SecurityUtils.getCurrentVendorId(), startDate, endDate, page, size));
    }
}
