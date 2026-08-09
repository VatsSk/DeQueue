package com.dequeue.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushSubscriptionRequest {
    @NotBlank
    private String orderId;
    @NotBlank
    private String sessionId;
    @NotBlank
    private String customerToken;   // HMAC token for validation
    @NotBlank
    private String endpoint;
    @NotBlank
    private String p256dh;
    @NotBlank
    private String auth;
}
