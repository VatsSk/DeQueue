package com.dequeue.common.event;

public interface EventListener {
    void onOrderEvent(OrderEvent event);
    void onQueueUpdate(QueueUpdateEvent event);
}
