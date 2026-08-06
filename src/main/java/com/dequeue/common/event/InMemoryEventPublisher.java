package com.dequeue.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryEventPublisher implements EventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Override
    public void publishOrderEvent(OrderEvent event) {
        log.debug("Publishing order event: {} for vendor: {}", event.getEventType(), event.getVendorId());
        applicationEventPublisher.publishEvent(event);
    }
    
    @Override
    public void publishQueueUpdate(QueueUpdateEvent event) {
        log.debug("Publishing queue update for vendor: {}", event.getVendorId());
        applicationEventPublisher.publishEvent(event);
    }
}
