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
    private com.dequeue.vendor.entity.VendorSettings settings;
}
