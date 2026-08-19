package com.dequeue.vendor.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorSettings {
    @Builder.Default
    private boolean allowCustomOrder = false;
    @Builder.Default
    private boolean autoAcceptOrders = false;
    @Builder.Default
    private int maxQueueSize = 50;
    @Builder.Default
    private int estimatedPrepTime = 10;
    @Builder.Default
    private boolean enableGeofence = false;
    @Builder.Default
    private String orderPrefix = "Q";
    @Builder.Default
    private String currency = "INR";
    @Builder.Default
    private Double taxPercentage = 0.0;
    
    @Builder.Default
    private String taxName = "Tax";
    
    private String gstNumber;

    @Builder.Default
    private boolean showPreparationTime = true;
    
    @Builder.Default
    private Double additionalCharges = 0.0;
    
    @Builder.Default
    private String additionalChargeName = "Service Charge";
    
    @Builder.Default
    private java.util.List<Coupon> coupons = new java.util.ArrayList<>();
    
    @Builder.Default
    private java.util.List<CustomFieldDef> customFields = new java.util.ArrayList<>();
}
