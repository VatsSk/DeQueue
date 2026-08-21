package com.dequeue.cashfree.controller;

import com.dequeue.cashfree.config.CashfreeProperties;
import com.dequeue.cashfree.dto.PaymentOrderRequest;
import com.dequeue.cashfree.dto.PaymentOrderResponse;
import com.dequeue.cashfree.service.CashfreePaymentService;
import com.dequeue.cashfree.service.CashfreeWebhookService;
import com.dequeue.cashfree.service.FinancialCalculationService;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.order.entity.Order;
import com.dequeue.order.repository.OrderRepository;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Cashfree payment order creation and webhook processing")
public class PaymentController {

    private final CashfreePaymentService cashfreePaymentService;
    private final CashfreeWebhookService cashfreeWebhookService;
    private final FinancialCalculationService financialCalculationService;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final CashfreeProperties cashfreeProperties;

    /**
     * Create a Cashfree payment order for a DeQueue order.
     * Called by the customer-facing checkout flow.
     * No auth required — customer is not authenticated.
     */
    @PostMapping("/create")
    @Operation(summary = "Create Cashfree payment order",
            description = "Creates a Cashfree payment session for a DeQueue order. Returns payment_session_id for Cashfree.js checkout.")
    public ApiResponse<PaymentOrderResponse> createPaymentOrder(
            @Valid @RequestBody PaymentOrderRequest request) {

        log.info("Creating payment order for DeQueue orderId={}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new com.dequeue.common.exception.ResourceNotFoundException(
                        "Order not found: " + request.getOrderId()));

        // Use order's total amount — not the client-provided amount (security)
        BigDecimal amount = order.getTotalAmount();

        // Create Cashfree payment order
        String cashfreeOrderId = "DQ-" + order.getId();
        Map<String, Object> cfResponse = cashfreePaymentService.createPaymentOrder(
                order.getId(), amount,
                request.getCustomerName(),
                request.getCustomerEmail(),
                request.getCustomerPhone(),
                request.getReturnUrl());

        // Store cashfreeOrderId on the DeQueue order
        order.setCashfreeOrderId(cashfreeOrderId);
        orderRepository.save(order);

        String sessionId = (String) cfResponse.get("payment_session_id");

        log.info("Payment order created: dequeueOrderId={}, cashfreeOrderId={}",
                order.getId(), cashfreeOrderId);

        return ApiResponse.success(PaymentOrderResponse.builder()
                .dequeueOrderId(order.getId())
                .cashfreeOrderId(cashfreeOrderId)
                .paymentSessionId(sessionId)
                .amount(amount)
                .currency("INR")
                .status("CREATED")
                .environment(cashfreeProperties.getEnvironment())
                .build());
    }

    /**
     * Cashfree webhook endpoint.
     * Must be public (no auth). Signature verification is done inside.
     */
    @PostMapping("/webhook/cashfree")
    @Operation(summary = "Cashfree webhook receiver",
            description = "Receives Cashfree payment/refund webhooks. Verifies signature before processing.")
    public ResponseEntity<String> cashfreeWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature,
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp) {

        log.info("Cashfree webhook received — verifying signature");

        if (!cashfreeWebhookService.verifySignature(rawBody, signature, timestamp)) {
            log.warn("Invalid Cashfree webhook signature — rejecting");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        cashfreeWebhookService.processWebhookEvent(rawBody);
        return ResponseEntity.ok("OK");
    }

    /**
     * Check payment status for a Cashfree order.
     */
    @GetMapping("/{cashfreeOrderId}/status")
    @Operation(summary = "Get Cashfree payment status")
    public ApiResponse<Object> getPaymentStatus(@PathVariable String cashfreeOrderId) {
        return ApiResponse.success(cashfreePaymentService.getPaymentStatus(cashfreeOrderId));
    }
}
