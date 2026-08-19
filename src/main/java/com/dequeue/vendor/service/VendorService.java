package com.dequeue.vendor.service;

import com.dequeue.vendor.dto.*;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.vendor.entity.VendorSettings;

public interface VendorService {
    VendorResponse getCurrentVendor(String userId);
    VendorResponse updateVendor(String userId, UpdateVendorRequest request);
    ShopStatus updateShopStatus(String userId, ShopStatusRequest request);
    ShopStatus getShopStatus(String userId);
    PublicVendorResponse getVendorByCode(String vendorCode);
    ShopStatus getVendorStatusByCode(String vendorCode);

    VendorSettings updateSettings(String vendorId, VendorSettingsDto settings);

    // Platform Admin Methods
    VendorResponse createVendor(CreateVendorRequest request);
    java.util.List<VendorResponse> getAllVendors();
    VendorResponse toggleVendorStatus(String vendorId, boolean active);
}
