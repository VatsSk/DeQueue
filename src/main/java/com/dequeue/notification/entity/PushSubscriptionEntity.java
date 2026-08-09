package com.dequeue.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "push_subscriptions")
@CompoundIndex(name = "order_session_idx", def = "{'orderId': 1, 'sessionId': 1}", unique = true)
public class PushSubscriptionEntity {
    @Id
    private String id;
    private String orderId;
    private String sessionId;
    private String endpoint;
    private String p256dh;   // Public key
    private String auth;     // Auth secret
    
    @CreatedDate
    private Instant createdAt;
}
