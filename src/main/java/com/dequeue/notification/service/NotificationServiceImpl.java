package com.dequeue.notification.service;

import com.dequeue.common.event.OrderEvent;
import com.dequeue.common.event.QueueUpdateEvent;
import com.dequeue.notification.dto.OrderStatusEvent;
import com.dequeue.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final RedisOrderEventPublisher redisOrderEventPublisher;

    @Override
    public void sendOrderUpdate(OrderEvent event) {
        log.info("Sending order update notification: {}", event);
    }
    
    @Override
    public void sendQueueUpdate(QueueUpdateEvent event) {
        log.info("Sending queue update notification: {}", event);
    }

    /**
     * Publish a customer-facing order status event through Redis.
     * This distributes the event to all backend instances for WebSocket + Web Push delivery.
     */
    public void publishCustomerOrderStatusEvent(Order order) {
        OrderStatusEvent event = OrderStatusEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .vendorId(order.getVendorId())
                .queueNumber(order.getQueueNumber())
                .sessionId(order.getSessionId())
                .status(order.getStatus().name())
                .message(getStatusMessage(order.getStatus().name(), order.getQueueNumber()))
                .timestamp(Instant.now())
                .build();

        redisOrderEventPublisher.publishOrderStatusEvent(event);
        log.info("Published customer order status event: orderId={}, status={}", order.getId(), order.getStatus());
    }

    private String getStatusMessage(String status, String queueNumber) {
        String orderRef = queueNumber != null ? queueNumber : "Your order";
        return switch (status) {
            case "PENDING" -> orderRef + " has been placed.";
            case "ACCEPTED" -> orderRef + " has been confirmed.";
            case "PREPARING" -> orderRef + " is being prepared.";
            case "READY" -> orderRef + " is ready for pickup!";
            case "COMPLETED" -> orderRef + " has been completed. Thank you!";
            case "CANCELLED" -> orderRef + " has been cancelled.";
            default -> orderRef + " status updated to " + status;
        };
    }
}
