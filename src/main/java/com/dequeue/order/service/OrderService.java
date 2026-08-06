package com.dequeue.order.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.order.dto.*;
import com.dequeue.order.entity.OrderStatus;
import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    PageResponse<OrderSummary> listOrders(String vendorId, OrderStatus status, LocalDate startDate, LocalDate endDate, String queueNumber, int page, int size);
    OrderResponse getOrder(String vendorId, String id);
    OrderResponse updateStatus(String vendorId, String id, StatusUpdateRequest request);
    List<OrderSummary> getActiveOrders(String vendorId);
    TodayOrdersSummary getTodaySummary(String vendorId);
    PageResponse<OrderSummary> getHistory(String vendorId, LocalDate startDate, LocalDate endDate, int page, int size);
    
    OrderResponse placeOrder(String vendorCode, PlaceOrderRequest request);
    TrackOrderResponse trackOrder(String vendorCode, String queueNumber);
    List<OrderSummary> getActiveSessionOrders(String vendorCode, String sessionId);
    OrderResponse placeCustomOrder(String vendorCode, CustomOrderRequest request);
}
