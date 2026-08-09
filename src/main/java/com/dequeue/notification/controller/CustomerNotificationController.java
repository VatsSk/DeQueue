package com.dequeue.notification.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.exception.UnauthorizedException;
import com.dequeue.notification.dto.PushSubscriptionRequest;
import com.dequeue.notification.entity.PushSubscriptionEntity;
import com.dequeue.notification.repository.PushSubscriptionRepository;
import com.dequeue.notification.service.CustomerSessionTokenService;
import com.dequeue.notification.service.WebPushService;
import com.dequeue.order.entity.Order;
import com.dequeue.order.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/notifications")
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final WebPushService webPushService;
    private final CustomerSessionTokenService tokenService;
    private final PushSubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/vapid-public-key")
    public ApiResponse<Map<String, String>> getVapidPublicKey() {
        return ApiResponse.success(Map.of(
            "publicKey", webPushService.getVapidPublicKey() != null ? webPushService.getVapidPublicKey() : ""
        ));
    }

    @PostMapping("/subscribe")
    public ApiResponse<String> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        log.info("Received push subscription request: {}", request);
        
        // Validate the customer token
        if (!tokenService.validateToken(request.getCustomerToken(), request.getOrderId(), request.getSessionId())) {
            throw new UnauthorizedException("Invalid customer session token");
        }

        // Verify order exists and belongs to this session
        Order order = orderRepository.findById(request.getOrderId())
                .filter(o -> request.getSessionId().equals(o.getSessionId()))
                .orElseThrow(() -> new UnauthorizedException("Order not found or session mismatch"));

        // Upsert subscription (reuse if already exists for this order+session)
        PushSubscriptionEntity subscription = subscriptionRepository
                .findByOrderIdAndSessionId(request.getOrderId(), request.getSessionId())
                .orElse(new PushSubscriptionEntity());

        subscription.setOrderId(request.getOrderId());
        subscription.setSessionId(request.getSessionId());
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuth(request.getAuth());

        subscriptionRepository.save(subscription);
        log.info("Push subscription saved for order: {}", request.getOrderId());

        return ApiResponse.success("Subscription saved");
    }

    @GetMapping("/order-status")
    public ApiResponse<Map<String, Object>> getOrderStatus(
            @RequestParam String orderId,
            @RequestParam String sessionId,
            @RequestParam String token) {

        if (!tokenService.validateToken(token, orderId, sessionId)) {
            throw new UnauthorizedException("Invalid customer session token");
        }

        Order order = orderRepository.findById(orderId)
                .filter(o -> sessionId.equals(o.getSessionId()))
                .orElseThrow(() -> new UnauthorizedException("Order not found or session mismatch"));

        return ApiResponse.success(Map.of(
            "orderId", order.getId(),
            "status", order.getStatus().name(),
            "queueNumber", order.getQueueNumber() != null ? order.getQueueNumber() : "",
            "updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : ""
        ));
    }
}
