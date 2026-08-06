package com.dequeue.notification.service;

import com.dequeue.common.event.OrderEvent;
import com.dequeue.common.event.QueueUpdateEvent;

public interface NotificationService {
    void sendOrderUpdate(OrderEvent event);
    void sendQueueUpdate(QueueUpdateEvent event);
}
