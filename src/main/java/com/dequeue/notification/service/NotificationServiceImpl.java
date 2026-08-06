package com.dequeue.notification.service;

import com.dequeue.common.event.OrderEvent;
import com.dequeue.common.event.QueueUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    @Override
    public void sendOrderUpdate(OrderEvent event) {
        log.info("Sending order update notification: {}", event);
    }
    
    @Override
    public void sendQueueUpdate(QueueUpdateEvent event) {
        log.info("Sending queue update notification: {}", event);
    }
}
