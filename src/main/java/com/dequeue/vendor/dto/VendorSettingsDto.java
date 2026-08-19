package com.dequeue.vendor.dto;

import lombok.Data;
import java.util.List;
import com.dequeue.vendor.entity.Coupon;

@Data
public class VendorSettingsDto {
    private boolean allowCustomOrder;
    private boolean autoAcceptOrders;
    private int maxQueueSize;
    private int estimatedPrepTime;
    private boolean enableGeofence;
    private String orderPrefix;
    private String currency;
    private Double taxPercentage;
    private boolean showPreparationTime;
    
    private Double additionalCharges;
    private String additionalChargeName;
    private List<Coupon> coupons;
    private String taxName;
    private String gstNumber;
    private List<com.dequeue.vendor.entity.CustomFieldDef> customFields;
}
