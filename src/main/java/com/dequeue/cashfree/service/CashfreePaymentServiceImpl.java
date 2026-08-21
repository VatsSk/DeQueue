package com.dequeue.cashfree.service;

import com.dequeue.cashfree.config.CashfreeProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Cashfree Payment Gateway service implementation.
 * Uses Java 21 HttpClient for HTTP calls.
 * No credentials are ever logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashfreePaymentServiceImpl implements CashfreePaymentService {

    private final CashfreeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Override
    public Map<String, Object> createPaymentOrder(
            String orderId, BigDecimal amount,
            String customerName, String customerEmail, String customerPhone,
            String returnUrl) {

        log.info("Creating Cashfree payment order for DeQueue orderId={}, amount={}", orderId, amount);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("order_id", "DQ-" + orderId);
            body.put("order_amount", amount);
            body.put("order_currency", "INR");

            Map<String, String> customer = new HashMap<>();
            customer.put("customer_id", "CUST-" + orderId.substring(0, Math.min(8, orderId.length())));
            customer.put("customer_name", customerName != null ? customerName : "Customer");
            customer.put("customer_email", customerEmail != null ? customerEmail : "customer@dequeue.app");
            customer.put("customer_phone", customerPhone != null ? customerPhone : "9999999999");
            body.put("customer_details", customer);

            if (returnUrl != null) {
                Map<String, String> meta = new HashMap<>();
                meta.put("return_url", returnUrl);
                body.put("order_meta", meta);
            }

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/pg/orders"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Cashfree createPaymentOrder response status={}", response.statusCode());

            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to create Cashfree payment order for orderId={}: {}", orderId, e.getMessage());
            throw new RuntimeException("Cashfree payment order creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getPaymentStatus(String cashfreeOrderId) {
        log.info("Getting Cashfree payment status for cashfreeOrderId={}", cashfreeOrderId);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/pg/orders/" + cashfreeOrderId))
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to get Cashfree payment status for {}: {}", cashfreeOrderId, e.getMessage());
            throw new RuntimeException("Cashfree payment status check failed", e);
        }
    }

    @Override
    public Object getPaymentsForOrder(String cashfreeOrderId) {
        log.info("Getting payments for cashfreeOrderId={}", cashfreeOrderId);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/pg/orders/" + cashfreeOrderId + "/payments"))
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), Object.class);
        } catch (Exception e) {
            log.error("Failed to get payments for order {}: {}", cashfreeOrderId, e.getMessage());
            throw new RuntimeException("Failed to get payments from Cashfree", e);
        }
    }

    @Override
    public Map<String, Object> createRefund(
            String cashfreeOrderId, String refundId, BigDecimal amount, String note) {
        log.info("Creating Cashfree refund: cashfreeOrderId={}, refundId={}, amount={}",
                cashfreeOrderId, refundId, amount);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("refund_amount", amount);
            body.put("refund_id", refundId);
            if (note != null) body.put("refund_note", note);

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/pg/orders/" + cashfreeOrderId + "/refunds"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Cashfree refund response status={} for orderId={}", response.statusCode(), cashfreeOrderId);
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Cashfree refund failed for order {}: {}", cashfreeOrderId, e.getMessage());
            throw new RuntimeException("Cashfree refund creation failed", e);
        }
    }
}
