package com.dequeue.notification.service;

import com.dequeue.notification.dto.OrderStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOrderEventSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;
    private final ObjectMapper objectMapper;

    // Statuses that warrant a customer-facing push notification
    private static final List<String> NOTIFIABLE_STATUSES = List.of(
        "ACCEPTED", "PREPARING", "READY", "CANCELLED"
    );

    /**
     * Called by RedisMessageListenerContainer when a message is received.
     */
    public void handleMessage(String message) {
        try {
            OrderStatusEvent event = objectMapper.readValue(message, OrderStatusEvent.class);
            log.info("Received order status event from Redis: orderId={}, status={}", event.getOrderId(), event.getStatus());

            // 1. Send to customer's STOMP topic (by queueNumber for live tracking)
            if (event.getQueueNumber() != null) {
                messagingTemplate.convertAndSend("/topic/orders/" + event.getQueueNumber(), Map.of(
                    "eventId", event.getEventId(),
                    "orderId", event.getOrderId(),
                    "status", event.getStatus(),
                    "message", event.getMessage(),
                    "queueNumber", event.getQueueNumber(),
                    "timestamp", event.getTimestamp().toString()
                ));
                log.debug("Sent order status to STOMP topic: /topic/orders/{}", event.getQueueNumber());
            }

            // 2. Send to customer-specific session topic for authenticated delivery
            if (event.getSessionId() != null && event.getOrderId() != null) {
                messagingTemplate.convertAndSend("/topic/customer/" + event.getSessionId() + "/" + event.getOrderId(), Map.of(
                    "eventId", event.getEventId(),
                    "orderId", event.getOrderId(),
                    "status", event.getStatus(),
                    "message", event.getMessage(),
                    "queueNumber", event.getQueueNumber() != null ? event.getQueueNumber() : "",
                    "timestamp", event.getTimestamp().toString()
                ));
            }

            // 3. Send Web Push notification for meaningful status changes
            if (NOTIFIABLE_STATUSES.contains(event.getStatus())) {
                String title = getNotificationTitle(event.getStatus());
                String body = event.getMessage();
                webPushService.sendPushNotification(event.getOrderId(), title, body, event.getStatus());
            }

        } catch (Exception e) {
            log.error("Failed to process Redis order status event", e);
        }
    }

    private String getNotificationTitle(String status) {
        return switch (status) {
            case "ACCEPTED" -> "\uD83D\uDFE2 Order Confirmed";
            case "PREPARING" -> "\uD83D\uDFE1 Order Update";
            case "READY" -> "\u2705 Order Ready";
            case "CANCELLED" -> "\u274C Order Cancelled";
            default -> "\uD83D\uDD14 Order Update";
        };
    }
}
