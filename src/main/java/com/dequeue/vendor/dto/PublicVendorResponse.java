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
    }
}
