package com.dequeue.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class CustomerSessionTokenService {

    private final String secret;

    public CustomerSessionTokenService(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    /**
     * Generate a signed token binding orderId and sessionId together.
     * Token = Base64(HMAC-SHA256(orderId|sessionId, secret))
     */
    public String generateToken(String orderId, String sessionId) {
        try {
            String data = orderId + "|" + sessionId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to generate customer session token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Validate that the provided token matches the orderId+sessionId pair.
     */
    public boolean validateToken(String token, String orderId, String sessionId) {
        if (token == null || orderId == null || sessionId == null) return false;
        String expected = generateToken(orderId, sessionId);
        return expected.equals(token);
    }
}
