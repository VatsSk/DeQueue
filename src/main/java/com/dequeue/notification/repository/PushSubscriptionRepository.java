package com.dequeue.notification.repository;

import com.dequeue.notification.entity.PushSubscriptionEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends MongoRepository<PushSubscriptionEntity, String> {
    Optional<PushSubscriptionEntity> findByOrderIdAndSessionId(String orderId, String sessionId);
    List<PushSubscriptionEntity> findByOrderId(String orderId);
    void deleteByOrderId(String orderId);
}
