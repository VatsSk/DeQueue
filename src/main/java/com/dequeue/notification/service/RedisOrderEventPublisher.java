package com.dequeue.notification.service;

import com.dequeue.notification.dto.OrderStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOrderEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${dequeue.notification.channel:dequeue:order-status}")
    private String channel;

    public void publishOrderStatusEvent(OrderStatusEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, message);
            log.debug("Published order status event to Redis: orderId={}, status={}", event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish order status event to Redis", e);
        }
    }
}
