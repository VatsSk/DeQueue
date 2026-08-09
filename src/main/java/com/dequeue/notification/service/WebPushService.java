package com.dequeue.notification.service;

import com.dequeue.notification.entity.PushSubscriptionEntity;
import com.dequeue.notification.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebPushService {

    @Value("${dequeue.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${dequeue.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${dequeue.vapid.subject:mailto:admin@dequeue.com}")
    private String vapidSubject;

    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private PushService pushService;
    private boolean enabled = false;

    public WebPushService(PushSubscriptionRepository subscriptionRepository, ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (vapidPublicKey == null || vapidPublicKey.isBlank() || vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            log.warn("VAPID keys not configured. Web Push notifications are disabled.");
            return;
        }
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
            enabled = true;
            log.info("Web Push service initialized successfully");
        } catch (GeneralSecurityException e) {
            log.error("Failed to initialize Web Push service", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Async
    public void sendPushNotification(String orderId, String title, String body, String status) {
        if (!enabled) {
            log.debug("Web Push not enabled, skipping notification for order: {}", orderId);
            return;
        }

        List<PushSubscriptionEntity> subscriptions = subscriptionRepository.findByOrderId(orderId);
        if (subscriptions.isEmpty()) {
            log.debug("No push subscriptions found for order: {}", orderId);
            return;
        }

        for (PushSubscriptionEntity sub : subscriptions) {
            try {
                Map<String, Object> payload = Map.of(
                    "title", title,
                    "body", body,
                    "orderId", orderId,
                    "status", status,
                    "timestamp", System.currentTimeMillis()
                );

                String payloadJson = objectMapper.writeValueAsString(payload);

                Subscription subscription = new Subscription(
                    sub.getEndpoint(),
                    new Subscription.Keys(sub.getP256dh(), sub.getAuth())
                );

                Notification notification = new Notification(
                    subscription,
                    payloadJson
                );

                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 410 || statusCode == 404) {
                    log.info("Push subscription expired/invalid for order: {}, removing", orderId);
                    subscriptionRepository.delete(sub);
                } else if (statusCode >= 400) {
                    log.warn("Push notification failed for order: {}, status: {}", orderId, statusCode);
                } else {
                    log.debug("Push notification sent for order: {}", orderId);
                }
            } catch (Exception e) {
                log.error("Failed to send push notification for order: {}", orderId, e);
            }
        }
    }
}
