package com.dequeue.order.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.order.dto.CustomOrderRequest;
import com.dequeue.order.dto.OrderResponse;
import com.dequeue.order.dto.OrderSummary;
import com.dequeue.order.dto.PlaceOrderRequest;
import com.dequeue.order.dto.TrackOrderResponse;
import com.dequeue.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/orders")
@RequiredArgsConstructor
public class PublicOrderController {
    private final OrderService orderService;

    @PostMapping("/{vendorCode}")
    public ApiResponse<OrderResponse> placeOrder(
            @PathVariable String vendorCode,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.success(orderService.placeOrder(vendorCode, request));
    }

    @GetMapping("/{vendorCode}/track/{queueNumber}")
    public ApiResponse<TrackOrderResponse> trackOrder(
            @PathVariable String vendorCode,
            @PathVariable String queueNumber) {
        return ApiResponse.success(orderService.trackOrder(vendorCode, queueNumber));
    }

    @GetMapping("/{vendorCode}/active")
    public ApiResponse<List<OrderSummary>> getActiveSessionOrders(
            @PathVariable String vendorCode,
            @RequestHeader("X-Session-ID") String sessionId) {
        return ApiResponse.success(orderService.getActiveSessionOrders(vendorCode, sessionId));
    }

    @PostMapping("/{vendorCode}/custom")
    public ApiResponse<OrderResponse> placeCustomOrder(
            @PathVariable String vendorCode,
            @Valid @RequestBody CustomOrderRequest request) {
        return ApiResponse.success(orderService.placeCustomOrder(vendorCode, request));
    }

    @GetMapping("/{vendorCode}/currently-serving")
    public ApiResponse<String> getCurrentlyServing(@PathVariable String vendorCode) {
        return ApiResponse.success(orderService.getCurrentlyServing(vendorCode));
    }

    @PostMapping("/{vendorCode}/cancel/{queueNumber}")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable String vendorCode,
            @PathVariable String queueNumber,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken) {
        return ApiResponse.success(orderService.cancelOrder(vendorCode, queueNumber, sessionToken));
    }

    @PostMapping("/{vendorCode}/feedback/{queueNumber}")
    public ApiResponse<OrderResponse> submitFeedback(
            @PathVariable String vendorCode,
            @PathVariable String queueNumber,
            @RequestBody com.dequeue.order.dto.FeedbackRequest request) {
        return ApiResponse.success(orderService.submitFeedback(vendorCode, queueNumber, request));
    }
}
