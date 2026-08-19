package com.dequeue.order.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.order.dto.*;
import com.dequeue.order.entity.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    PageResponse<OrderSummary> listOrders(String vendorId, OrderStatus status, LocalDate startDate, LocalDate endDate,
                                          String queueNumber, int page, int size,
                                          List<OrderStatus> visibilityFilter);

    OrderResponse getOrder(String vendorId, String id);

    /** Generic status update — enforces state machine. Kept for compatibility. */
    OrderResponse updateStatus(String vendorId, String id, StatusUpdateRequest request,
                               String staffId, String staffName);

    /** Dedicated workflow action methods */
    OrderResponse acceptOrder(String vendorId, String id, String staffId, String staffName);
    OrderResponse prepareOrder(String vendorId, String id, String staffId, String staffName);
    OrderResponse markReady(String vendorId, String id, String staffId, String staffName);
    OrderResponse completeOrder(String vendorId, String id, String staffId, String staffName);
    OrderResponse cancelOrder(String vendorId, String id, String staffId, String staffName, String note);

    List<OrderResponse> getActiveOrders(String vendorId, List<OrderStatus> visibilityFilter);

    TodayOrdersSummary getTodaySummary(String vendorId);

    PageResponse<OrderSummary> getHistory(String vendorId, LocalDate startDate, LocalDate endDate, int page, int size);

    OrderResponse placeOrder(String vendorCode, PlaceOrderRequest request);
    TrackOrderResponse trackOrder(String vendorCode, String queueNumber);
    List<OrderSummary> getActiveSessionOrders(String vendorCode, String sessionId);
    OrderResponse placeCustomOrder(String vendorCode, CustomOrderRequest request);
    String getCurrentlyServing(String vendorCode);
    OrderResponse cancelOrder(String vendorCode, String queueNumber, String sessionToken);
    OrderResponse submitFeedback(String vendorCode, String queueNumber, FeedbackRequest request);
}
