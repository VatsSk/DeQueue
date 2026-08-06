package com.dequeue.vendor.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.vendor.entity.GeoLocation;
import com.dequeue.vendor.entity.VendorSettings;

@Data
public class VendorResponse {
    private String id;
    private String vendorCode;
    private String shopName;
    private String ownerName;
    private String email;
    private String phone;
    private String logo;
    private String banner;
    private AddressDto address;
    private List<BusinessHourDto> businessHours;
    private ShopStatus shopStatus;
    private GeoLocation geoLocation;
    private Double geoRadius;
    private VendorSettings settings;
    private Instant createdAt;
}

