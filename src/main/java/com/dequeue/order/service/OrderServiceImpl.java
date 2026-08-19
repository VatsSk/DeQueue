package com.dequeue.order.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.event.EventPublisher;
import com.dequeue.common.event.OrderEvent;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.util.QueueNumberGenerator;
import com.dequeue.menu.entity.MenuItem;
import com.dequeue.menu.repository.MenuItemRepository;
import com.dequeue.notification.service.CustomerSessionTokenService;
import com.dequeue.notification.service.NotificationServiceImpl;
import com.dequeue.order.dto.*;
import com.dequeue.order.entity.*;
import com.dequeue.order.mapper.OrderMapper;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.repository.VendorRepository;
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
    private final com.dequeue.menu.repository.CustomizationGroupRepository customizationGroupRepository;
    private final NotificationServiceImpl notificationServiceImpl;
    private final CustomerSessionTokenService customerSessionTokenService;

    // ────────────────── Staff-facing: list & get ──────────────────

    @Override
    public PageResponse<OrderSummary> listOrders(String vendorId, OrderStatus status,
                                                  LocalDate startDate, LocalDate endDate,
                                                  String queueNumber, int page, int size,
                                                  List<OrderStatus> visibilityFilter) {
        Page<Order> orders;
        if (queueNumber != null && !queueNumber.isEmpty()) {
            orders = orderRepository.findByVendorIdAndQueueNumber(vendorId, queueNumber, PageRequest.of(page, size));
        } else if (status != null) {
            // Intersect requested status with visibility filter
            if (visibilityFilter != null && !visibilityFilter.isEmpty() && !visibilityFilter.contains(status)) {
                // The requested status is not in the user's visibility — return empty
                return PageResponse.empty();
            }
            orders = orderRepository.findByVendorIdAndStatus(vendorId, status, PageRequest.of(page, size));
        } else if (visibilityFilter != null && !visibilityFilter.isEmpty()) {
            orders = orderRepository.findByVendorIdAndStatusIn(vendorId, visibilityFilter, PageRequest.of(page, size));
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
    public OrderResponse updateStatus(String vendorId, String id, StatusUpdateRequest request,
                                      String staffId, String staffName) {
        Order order = getVendorOrder(vendorId, id);

        // Validate the state machine transition
        OrderStateMachine.validate(order.getStatus(), request.getStatus());

        return performTransition(order, request.getStatus(), staffId, staffName, request.getNote());
    }

    // ────────────────── Dedicated action endpoints ──────────────────

    @Override
    @Transactional
    public OrderResponse acceptOrder(String vendorId, String id, String staffId, String staffName) {
        Order order = getVendorOrder(vendorId, id);
        OrderStateMachine.validate(order.getStatus(), OrderStatus.ACCEPTED);
        return performTransition(order, OrderStatus.ACCEPTED, staffId, staffName, null);
    }

    @Override
    @Transactional
    public OrderResponse prepareOrder(String vendorId, String id, String staffId, String staffName) {
        Order order = getVendorOrder(vendorId, id);
        OrderStateMachine.validate(order.getStatus(), OrderStatus.PREPARING);
        order.setPreparationStartedAt(Instant.now());
        return performTransition(order, OrderStatus.PREPARING, staffId, staffName, null);
    }

    @Override
    @Transactional
    public OrderResponse markReady(String vendorId, String id, String staffId, String staffName) {
        Order order = getVendorOrder(vendorId, id);
        OrderStateMachine.validate(order.getStatus(), OrderStatus.READY);
        return performTransition(order, OrderStatus.READY, staffId, staffName, null);
    }

    @Override
    @Transactional
    public OrderResponse completeOrder(String vendorId, String id, String staffId, String staffName) {
        Order order = getVendorOrder(vendorId, id);
        OrderStateMachine.validate(order.getStatus(), OrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        return performTransition(order, OrderStatus.COMPLETED, staffId, staffName, null);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String vendorId, String id, String staffId, String staffName, String note) {
        Order order = getVendorOrder(vendorId, id);
        OrderStateMachine.validate(order.getStatus(), OrderStatus.CANCELLED);
        order.setCompletedAt(Instant.now());
        return performTransition(order, OrderStatus.CANCELLED, staffId, staffName, note);
    }

    // ────────────────── Active orders with visibility filter ──────────────────

    @Override
    public List<OrderResponse> getActiveOrders(String vendorId, List<OrderStatus> visibilityFilter) {
        List<OrderStatus> statuses;
        if (visibilityFilter != null && !visibilityFilter.isEmpty()) {
            statuses = visibilityFilter;
        } else {
            statuses = List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED,
                    OrderStatus.PREPARING, OrderStatus.READY);
        }
        List<Order> orders = orderRepository.findByVendorIdAndStatusIn(vendorId, statuses);
        return orderMapper.toResponseList(orders);
    }

    // ────────────────── Analytics ──────────────────

    @Override
    public TodayOrdersSummary getTodaySummary(String vendorId) {
        TodayOrdersSummary summary = new TodayOrdersSummary();
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Order> orders = orderRepository.findByVendorIdAndCreatedAtAfter(vendorId, startOfDay);
        summary.setTotalOrders(orders.size());
        summary.setTotalRevenue(orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return summary;
    }

    @Override
    public PageResponse<OrderSummary> getHistory(String vendorId, LocalDate startDate, LocalDate endDate, int page, int size) {
        return listOrders(vendorId, null, startDate, endDate, null, page, size, null);
    }

    // ────────────────── Customer-facing ──────────────────

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
        
        if (request.getMetadata() != null) {
            order.setMetadata(request.getMetadata());
        }

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
                
                List<SelectedCustomization> selectedCustomizations = new ArrayList<>();
                BigDecimal unitPriceWithCustomizations = menuItem.getPrice() != null ? menuItem.getPrice() : BigDecimal.ZERO;
                
                if (itemReq.getCustomizations() != null) {
                    for (CustomizationRequest custReq : itemReq.getCustomizations()) {
                        com.dequeue.menu.entity.CustomizationGroup group = customizationGroupRepository.findById(custReq.getGroupId())
                                .orElseThrow(() -> new BadRequestException("Customization group not found: " + custReq.getGroupId()));
                                
                        List<SelectedOption> selectedOptions = new ArrayList<>();
                        if (custReq.getSelectedOptionNames() != null) {
                            for (String optionName : custReq.getSelectedOptionNames()) {
                                com.dequeue.menu.entity.CustomizationOption option = group.getOptions().stream()
                                        .filter(o -> o.getName().equals(optionName))
                                        .findFirst()
                                        .orElseThrow(() -> new BadRequestException("Option not found: " + optionName));
                                
                                selectedOptions.add(SelectedOption.builder()
                                        .name(option.getName())
                                        .additionalPrice(option.getAdditionalPrice())
                                        .build());
                                        
                                if (option.getAdditionalPrice() != null) {
                                    unitPriceWithCustomizations = unitPriceWithCustomizations.add(option.getAdditionalPrice());
                                }
                            }
                        }
                        
                        selectedCustomizations.add(SelectedCustomization.builder()
                                .groupId(group.getId())
                                .groupName(group.getName())
                                .selectedOptions(selectedOptions)
                                .build());
                    }
                }
                
                orderItem.setSelectedCustomizations(selectedCustomizations);
                orderItem.setTotalPrice(unitPriceWithCustomizations.multiply(new BigDecimal(itemReq.getQuantity())));
                orderItem.setSpecialInstructions(itemReq.getSpecialInstructions());

                totalAmount = totalAmount.add(orderItem.getTotalPrice());
                items.add(orderItem);
            }
        }

        order.setOrderItems(items);
        order.setTotalAmount(totalAmount);

        order = orderRepository.save(order);
        eventPublisher.publishOrderEvent(new OrderEvent(vendor.getId(), order.getId(),
                order.getStatus().name(), order.getQueueNumber(), Instant.now(), OrderEvent.EventType.ORDER_PLACED));
        notificationServiceImpl.publishCustomerOrderStatusEvent(order);

        OrderResponse response = orderMapper.toResponse(order);
        response.setSessionId(order.getSessionId());
        response.setCustomerSessionToken(customerSessionTokenService.generateToken(order.getId(), order.getSessionId()));
        return response;
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
        
        if (request.getMetadata() != null) {
            order.setMetadata(request.getMetadata());
        }
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());

        order = orderRepository.save(order);
        eventPublisher.publishOrderEvent(new OrderEvent(vendor.getId(), order.getId(),
                order.getStatus().name(), order.getQueueNumber(), Instant.now(), OrderEvent.EventType.ORDER_PLACED));

        return orderMapper.toResponse(order);
    }

    @Override
    public String getCurrentlyServing(String vendorCode) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        List<Order> servingOrders = orderRepository.findByVendorIdAndStatusIn(
                vendor.getId(), List.of(OrderStatus.READY, OrderStatus.PREPARING));

        return servingOrders.stream()
                .max(java.util.Comparator.comparing(Order::getCreatedAt))
                .map(Order::getQueueNumber)
                .orElse(null);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String vendorCode, String queueNumber, String sessionToken) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        Order order = orderRepository.findByVendorIdAndQueueNumber(vendor.getId(), queueNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!customerSessionTokenService.validateToken(sessionToken, order.getId(), order.getSessionId())) {
            throw new BadRequestException("Invalid session token");
        }

        OrderStateMachine.validate(order.getStatus(), OrderStatus.CANCELLED);

        order.setCompletedAt(Instant.now());
        return performTransition(order, OrderStatus.CANCELLED, "customer", "Customer", null);
    }

    // ────────────────── private helpers ──────────────────

    private Order getVendorOrder(String vendorId, String orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getVendorId().equals(vendorId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /**
     * Core transition method. Validates no further state machine rules, sets the new status,
     * appends a StatusChange to the history, saves, and publishes events.
     */
    private OrderResponse performTransition(Order order, OrderStatus newStatus,
                                            String staffId, String staffName, String note) {
        OrderStatus previousStatus = order.getStatus();

        // Record status history
        StatusChange change = StatusChange.builder()
                .fromStatus(previousStatus)
                .toStatus(newStatus)
                .changedBy(staffId != null ? staffId : "system")
                .changedByName(staffName != null ? staffName : "system")
                .changedAt(Instant.now())
                .note(note)
                .build();

        if (order.getStatusHistory() == null) {
            order.setStatusHistory(new ArrayList<>());
        }
        order.getStatusHistory().add(change);

        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());

        order = orderRepository.save(order);

        log.info("Order {} transitioned: {} → {} by {}", order.getId(), previousStatus, newStatus, staffId);

        eventPublisher.publishOrderEvent(new OrderEvent(
                order.getVendorId(), order.getId(), order.getStatus().name(),
                order.getQueueNumber(), Instant.now(), OrderEvent.EventType.STATUS_CHANGED));
        notificationServiceImpl.publishCustomerOrderStatusEvent(order);

        return orderMapper.toResponse(order);
    }
}
