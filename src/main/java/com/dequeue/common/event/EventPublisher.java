package com.dequeue.common.event;

public interface EventPublisher {
    void publishOrderEvent(OrderEvent event);
    void publishQueueUpdate(QueueUpdateEvent event);
}
