package com.dequeue.config;

import com.dequeue.common.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received OrderEvent to push to WebSocket: {}", event);
        // Push to vendor topic
        messagingTemplate.convertAndSend("/topic/vendor/" + event.getVendorId(), event);
        
        // Push to specific order topic for tracking
        messagingTemplate.convertAndSend("/topic/orders/" + event.getQueueNumber(), event);
    }
}
