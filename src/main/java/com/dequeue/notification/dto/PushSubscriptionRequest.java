package com.dequeue.notification.dto;

import lombok.Data;

@Data
public class PushSubscriptionRequest {
    private String orderId;
    private String sessionId;
    private String customerToken;   // HMAC token for validation
    private String endpoint;
    private String p256dh;
    private String auth;
}
