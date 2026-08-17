package com.dequeue.order.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.order.dto.*;
import com.dequeue.order.entity.OrderStatus;
import com.dequeue.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    // ────────────────── List & Get ──────────────────

    @GetMapping
    @PreAuthorize("hasPermission(null, 'order.view')")
    @Operation(summary = "List orders (filtered by caller's visibility)")
    public ApiResponse<PageResponse<OrderSummary>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String queueNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.listOrders(
                SecurityUtils.getCurrentVendorId(), status, startDate, endDate, queueNumber, page, size,
                SecurityUtils.getCurrentOrderVisibilityStatuses()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'order.view')")
    public ApiResponse<OrderResponse> getOrder(@PathVariable String id) {
        return ApiResponse.success(orderService.getOrder(SecurityUtils.getCurrentVendorId(), id));
    }

    @GetMapping("/active")
    @PreAuthorize("hasPermission(null, 'order.view')")
    @Operation(summary = "Get active orders visible to the caller's role")
    public ApiResponse<List<OrderSummary>> getActiveOrders() {
        return ApiResponse.success(orderService.getActiveOrders(
                SecurityUtils.getCurrentVendorId(),
                SecurityUtils.getCurrentOrderVisibilityStatuses()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasPermission(null, 'order.view')")
    public ApiResponse<TodayOrdersSummary> getTodaySummary() {
        return ApiResponse.success(orderService.getTodaySummary(SecurityUtils.getCurrentVendorId()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasPermission(null, 'order.view')")
    public ApiResponse<PageResponse<OrderSummary>> getHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.getHistory(
                SecurityUtils.getCurrentVendorId(), startDate, endDate, page, size));
    }

    // ────────────────── Generic status update (backward compat, state machine enforced) ──────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update order status (state machine enforced). Prefer dedicated action endpoints.")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ApiResponse.success(orderService.updateStatus(
                SecurityUtils.getCurrentVendorId(), id, request,
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUserName()));
    }

    // ────────────────── Dedicated action endpoints ──────────────────

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasPermission(null, 'order.accept')")
    @Operation(summary = "Accept a PENDING order → ACCEPTED")
    public ApiResponse<OrderResponse> acceptOrder(@PathVariable String id) {
        return ApiResponse.success(orderService.acceptOrder(
                SecurityUtils.getCurrentVendorId(), id,
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUserName()));
    }

    @PostMapping("/{id}/prepare")
    @PreAuthorize("hasPermission(null, 'order.prepare')")
    @Operation(summary = "Start preparing an ACCEPTED order → PREPARING")
    public ApiResponse<OrderResponse> prepareOrder(@PathVariable String id) {
        return ApiResponse.success(orderService.prepareOrder(
                SecurityUtils.getCurrentVendorId(), id,
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUserName()));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasPermission(null, 'order.ready')")
    @Operation(summary = "Mark a PREPARING order as READY")
    public ApiResponse<OrderResponse> markReady(@PathVariable String id) {
        return ApiResponse.success(orderService.markReady(
                SecurityUtils.getCurrentVendorId(), id,
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUserName()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasPermission(null, 'order.complete')")
    @Operation(summary = "Complete a READY order → COMPLETED")
    public ApiResponse<OrderResponse> completeOrder(@PathVariable String id) {
        return ApiResponse.success(orderService.completeOrder(
                SecurityUtils.getCurrentVendorId(), id,
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUserName()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasPermission(null, 'order.cancel')")
    @Operation(summary = "Cancel an order (allowed from PENDING, ACCEPTED, PREPARING)")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable String id,
            @RequestBody(required = false) StatusUpdateRequest request) {
        String note = request != null ? request.getNote() : null;
        return ApiResponse.success(orderService.cancelOrder(
                SecurityUtils.getCurrentVendorId(), id,
                SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUserName(), note));
    }
}
