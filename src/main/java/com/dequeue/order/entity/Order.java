package com.dequeue.order.entity;

import com.dequeue.settlement.entity.PaymentSource;
import com.dequeue.settlement.entity.SettlementStatus;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class
Order {
    @Id
    private String id;
    
    @Indexed
    private String vendorId;
    
    private String vendorCode;
    private String sessionId;
    
    @Builder.Default
    private java.util.Map<String, String> metadata = new java.util.HashMap<>();
    
    @Builder.Default
    private java.util.Map<String, String> customFields = new java.util.HashMap<>();
    
    private String queueNumber;
    
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
    
    private String customOrderText;
    private BigDecimal totalAmount;
    
    private BigDecimal subtotal;
    private String couponCode;
    private BigDecimal couponDiscount;
    private String taxName;
    private BigDecimal taxAmount;
    private String serviceChargeName;
    private BigDecimal serviceChargeAmount;
    
    @Indexed
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    private String customerNote;
    
    @Builder.Default
    private List<StatusChange> statusHistory = new ArrayList<>();
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    private Instant completedAt;
    private Instant estimatedReadyTime;
    private Instant preparationStartedAt;
    
    private Integer rating;
    private String feedback;

    // ── Payment & Settlement fields ──────────────────────────────────────────

    /**
     * How this order was paid. Set when payment is recorded (Cashfree callback
     * or offline payment entry). Null = not yet paid.
     */
    @Indexed
    private PaymentSource paymentSource;

    /** ID of the PaymentTransaction document for this order. */
    private String paymentTransactionId;

    // Fee snapshot — immutable once payment is finalized

    /**
     * DeQueue platform commission percentage captured at time of finalization.
     * Historical orders retain the original rate even if the platform rate changes.
     */
    @Builder.Default
    private BigDecimal platformFeePercentage = BigDecimal.ZERO;

    /** Calculated platform fee: totalAmount × platformFeePercentage / 100. */
    @Builder.Default
    private BigDecimal platformFeeAmount = BigDecimal.ZERO;

    /**
     * Cashfree gateway fee snapshot. Zero for cash/offline orders.
     */
    @Builder.Default
    private BigDecimal cashfreeFee = BigDecimal.ZERO;

    /**
     * Cashfree tax (GST on gateway fee) snapshot. Zero for cash/offline orders.
     */
    @Builder.Default
    private BigDecimal cashfreeTax = BigDecimal.ZERO;

    /** Refund amount applied to this order. */
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /**
     * Net amount due to vendor for this order:
     *   totalAmount − cashfreeFee − cashfreeTax − platformFeeAmount − refundAmount
     */
    @Builder.Default
    private BigDecimal vendorNetAmount = BigDecimal.ZERO;

    // Settlement tracking

    /** ID of the VendorSettlement that includes this order. Null = not yet settled. */
    @Indexed
    private String settlementId;

    /** Settlement status for this order. */
    @Builder.Default
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    /** When this order was included in a settlement. */
    private Instant settledAt;
}
