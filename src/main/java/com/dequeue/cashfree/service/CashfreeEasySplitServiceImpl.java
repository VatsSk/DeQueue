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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashfreeEasySplitServiceImpl implements CashfreeEasySplitService {

    private final CashfreeProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Override
    public Map<String, Object> createVendor(String mongoVendorId, Map<String, Object> vendorDetails) {
        String cashfreeVendorId = CashfreeEasySplitService.buildCashfreeVendorId(mongoVendorId);
        log.info("Creating Cashfree Easy Split vendor: mongoVendorId={}, cashfreeVendorId={}",
                mongoVendorId, cashfreeVendorId);

        try {
            vendorDetails.put("vendor_id", cashfreeVendorId);
            String json = objectMapper.writeValueAsString(vendorDetails);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/easy-split/vendors"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Cashfree createVendor response status={} vendorId={}", response.statusCode(), cashfreeVendorId);
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to create Cashfree vendor {}: {}", cashfreeVendorId, e.getMessage());
            throw new RuntimeException("Cashfree vendor creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getVendor(String cashfreeVendorId) {
        log.info("Getting Cashfree vendor status: cashfreeVendorId={}", cashfreeVendorId);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/easy-split/vendors/" + cashfreeVendorId))
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to get Cashfree vendor {}: {}", cashfreeVendorId, e.getMessage());
            throw new RuntimeException("Cashfree vendor lookup failed", e);
        }
    }

    @Override
    public Map<String, Object> updateVendor(String cashfreeVendorId, Map<String, Object> vendorDetails) {
        log.info("Updating Cashfree vendor: cashfreeVendorId={}", cashfreeVendorId);
        try {
            String json = objectMapper.writeValueAsString(vendorDetails);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/easy-split/vendors/" + cashfreeVendorId))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Cashfree updateVendor response status={} vendorId={}", response.statusCode(), cashfreeVendorId);
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to update Cashfree vendor {}: {}", cashfreeVendorId, e.getMessage());
            throw new RuntimeException("Cashfree vendor update failed", e);
        }
    }

    @Override
    public Map<String, Object> createSplitAfterPayment(
            String cashfreeOrderId,
            String cashfreeVendorId,
            BigDecimal vendorAmount,
            BigDecimal platformAmount) {

        log.info("Creating Easy Split: cashfreeOrderId={}, cashfreeVendorId={}, vendorAmount={}, platformAmount={}",
                cashfreeOrderId, cashfreeVendorId, vendorAmount, platformAmount);

        try {
            // Build split detail per Cashfree Easy Split API
            Map<String, Object> splitDetail = new HashMap<>();
            splitDetail.put("vendor_id", cashfreeVendorId);
            splitDetail.put("amount", vendorAmount);
            splitDetail.put("percentage", null); // We use fixed amount split

            Map<String, Object> body = new HashMap<>();
            body.put("split", List.of(splitDetail));

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/easy-split/orders/" + cashfreeOrderId + "/split"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Easy Split createSplit response status={} for orderId={}",
                    response.statusCode(), cashfreeOrderId);
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to create Easy Split for order {}: {}", cashfreeOrderId, e.getMessage());
            throw new RuntimeException("Easy Split creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getSplitByOrderId(String cashfreeOrderId) {
        log.info("Getting Easy Split for cashfreeOrderId={}", cashfreeOrderId);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEffectiveBaseUrl() + "/easy-split/orders/" + cashfreeOrderId + "/split"))
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to get Easy Split for order {}: {}", cashfreeOrderId, e.getMessage());
            throw new RuntimeException("Easy Split lookup failed", e);
        }
    }

    @Override
    public Map<String, Object> getVendorSettlements(String cashfreeVendorId, String from, String to) {
        log.info("Getting vendor settlements: cashfreeVendorId={}, from={}, to={}",
                cashfreeVendorId, from, to);
        try {
            String url = properties.getEffectiveBaseUrl() + "/easy-split/vendors/" + cashfreeVendorId + "/settlements";
            if (from != null && to != null) {
                url += "?from=" + from + "&to=" + to;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-client-id", properties.getClientId())
                    .header("x-client-secret", properties.getClientSecret())
                    .header("x-api-version", properties.getApiVersion())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to get vendor settlements for {}: {}", cashfreeVendorId, e.getMessage());
            throw new RuntimeException("Vendor settlements lookup failed", e);
        }
    }
}
