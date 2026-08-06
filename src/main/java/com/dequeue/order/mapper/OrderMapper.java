package com.dequeue.order.mapper;

import com.dequeue.order.dto.*;
import com.dequeue.order.entity.Order;
import com.dequeue.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "items", source = "orderItems")
    OrderResponse toResponse(Order order);
    List<OrderResponse> toResponseList(List<Order> orders);
    
    @Mapping(target = "itemCount", expression = "java(order.getOrderItems() != null ? order.getOrderItems().size() : 0)")
    OrderSummary toSummary(Order order);
    List<OrderSummary> toSummaryList(List<Order> orders);
    
    OrderItemResponse toItemResponse(OrderItem item);
}
