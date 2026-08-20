package com.dequeue.vendor.dto;

import lombok.Data;
import java.util.List;
import com.dequeue.vendor.entity.ShopStatus;

@Data
public class PublicVendorResponse {
    private String id;
    private String vendorCode;
    private String shopName;
    private String logo;
    private String banner;
    private ShopStatus shopStatus;
    private List<BusinessHourDto> businessHours;
    private PublicSettingsDto settings;
    
    @Data
    public static class PublicSettingsDto {
        private boolean allowCustomOrder;
        private boolean showPreparationTime;
        private String currency;
        private boolean enableGeofence;
        private java.util.List<com.dequeue.vendor.entity.CustomFieldDef> customFields;
        private String gstNumber;
        private Double taxPercentage;
        private String taxName;
        private Double additionalCharges;
        private String additionalChargeName;
        private java.util.List<com.dequeue.vendor.entity.Coupon> coupons;
        private boolean enableOnlinePayment;
        private String upiId;
    }
}
