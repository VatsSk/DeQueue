package com.dequeue.order.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.event.OrderEvent;
import com.dequeue.common.event.EventPublisher;
import com.dequeue.common.util.QueueNumberGenerator;
import com.dequeue.order.dto.*;
import com.dequeue.order.entity.*;
import com.dequeue.order.mapper.OrderMapper;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.vendor.repository.VendorRepository;
import com.dequeue.menu.repository.MenuItemRepository;
import com.dequeue.menu.entity.MenuItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final OrderMapper orderMapper;
    private final EventPublisher eventPublisher;
    private final QueueNumberGenerator queueNumberGenerator;
    private final MenuItemRepository menuItemRepository;

    @Override
    public PageResponse<OrderSummary> listOrders(String vendorId, OrderStatus status, LocalDate startDate, LocalDate endDate, String queueNumber, int page, int size) {
        Page<Order> orders;
        if (queueNumber != null && !queueNumber.isEmpty()) {
            orders = orderRepository.findByVendorIdAndQueueNumber(vendorId, queueNumber, PageRequest.of(page, size));
        } else if (status != null) {
            orders = orderRepository.findByVendorIdAndStatus(vendorId, status, PageRequest.of(page, size));
        } else {
            orders = orderRepository.findByVendorId(vendorId, PageRequest.of(page, size));
        }
        
        return new PageResponse<>(
            orderMapper.toSummaryList(orders.getContent()),
            orders.getNumber(),
            orders.getSize(),
            orders.getTotalElements(),
            orders.getTotalPages(),
            orders.isLast()
        );
    }

    @Override
    public OrderResponse getOrder(String vendorId, String id) {
        Order order = orderRepository.findById(id)
            .filter(o -> o.getVendorId().equals(vendorId))
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(String vendorId, String id, StatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
            .filter(o -> o.getVendorId().equals(vendorId))
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            
        // Validate transition (simple mock validation)
        order.setStatus(request.getStatus());
        order.setUpdatedAt(Instant.now());
        
        if (request.getStatus() == OrderStatus.COLLECTED || request.getStatus() == OrderStatus.CANCELLED) {
            order.setCompletedAt(Instant.now());
        }
        order = orderRepository.save(order);
        eventPublisher.publishOrderEvent(new OrderEvent(vendorId, order.getId(), order.getStatus().name(), order.getQueueNumber(), Instant.now(), OrderEvent.EventType.STATUS_CHANGED));
        
        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderSummary> getActiveOrders(String vendorId) {
        List<Order> orders = orderRepository.findByVendorIdAndStatusIn(
            vendorId, 
            List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY)
        );
        return orderMapper.toSummaryList(orders);
    }

    @Override
    public TodayOrdersSummary getTodaySummary(String vendorId) {
        TodayOrdersSummary summary = new TodayOrdersSummary();
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Order> orders = orderRepository.findByVendorIdAndCreatedAtAfter(vendorId, startOfDay);
        int total = orders.size();
        summary.setTotalOrders(total);
        summary.setTotalRevenue(orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    @Override
    public PageResponse<OrderSummary> getHistory(String vendorId, LocalDate startDate, LocalDate endDate, int page, int size) {
        return listOrders(vendorId, null, startDate, endDate, null, page, size);
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(String vendorCode, PlaceOrderRequest request) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
            
        if (vendor.getShopStatus() != ShopStatus.OPEN) {
            throw new BadRequestException("Shop is currently closed");
        }
        
        Order order = new Order();
        order.setVendorId(vendor.getId());
        order.setVendorCode(vendorCode);
        order.setQueueNumber(queueNumberGenerator.generateQueueNumber(vendor.getId(), "Q"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setSessionId(request.getSessionId());
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        
        if (request.getItems() != null) {
            for (OrderItemRequest itemReq : request.getItems()) {
                MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new BadRequestException("Menu item not found: " + itemReq.getMenuItemId()));
                    
                OrderItem orderItem = new OrderItem();
                orderItem.setMenuItemId(menuItem.getId());
                orderItem.setMenuItemName(menuItem.getName());
                orderItem.setQuantity(itemReq.getQuantity());
                orderItem.setUnitPrice(menuItem.getPrice());
                orderItem.setTotalPrice(menuItem.getPrice().multiply(new BigDecimal(itemReq.getQuantity())));
                orderItem.setSpecialInstructions(itemReq.getSpecialInstructions());
                
                // Simplified customization mapping for now
                orderItem.setSelectedCustomizations(new ArrayList<>());
                
                totalAmount = totalAmount.add(orderItem.getTotalPrice());
                items.add(orderItem);
            }
        }
        
        order.setOrderItems(items);
        order.setTotalAmount(totalAmount);
        
        order = orderRepository.save(order);
        eventPublisher.publishOrderEvent(new OrderEvent(vendor.getId(), order.getId(), order.getStatus().name(), order.getQueueNumber(), Instant.now(), OrderEvent.EventType.ORDER_PLACED));
        
        return orderMapper.toResponse(order);
    }

    @Override
    public TrackOrderResponse trackOrder(String vendorCode, String queueNumber) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
            
        Order order = orderRepository.findByVendorIdAndQueueNumber(vendor.getId(), queueNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            
        TrackOrderResponse response = new TrackOrderResponse();
        response.setQueueNumber(order.getQueueNumber());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    @Override
    public List<OrderSummary> getActiveSessionOrders(String vendorCode, String sessionId) {
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public OrderResponse placeCustomOrder(String vendorCode, CustomOrderRequest request) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
            .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
            
        Order order = new Order();
        order.setVendorId(vendor.getId());
        order.setVendorCode(vendorCode);
        order.setQueueNumber(queueNumberGenerator.generateQueueNumber(vendor.getId(), "Q"));
        order.setCustomOrderText(request.getText());
        order.setCustomerNote(request.getCustomerNote());
        order.setSessionId(request.getSessionId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        
        order = orderRepository.save(order);
        eventPublisher.publishOrderEvent(new OrderEvent(vendor.getId(), order.getId(), order.getStatus().name(), order.getQueueNumber(), Instant.now(), OrderEvent.EventType.ORDER_PLACED));
        
        return orderMapper.toResponse(order);
    }
}
