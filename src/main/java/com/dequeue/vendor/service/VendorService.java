package com.dequeue.vendor.service;

import com.dequeue.vendor.dto.*;
import com.dequeue.vendor.entity.ShopStatus;

public interface VendorService {
    VendorResponse getCurrentVendor(String userId);
    VendorResponse updateVendor(String userId, UpdateVendorRequest request);
    ShopStatus updateShopStatus(String userId, ShopStatusRequest request);
    ShopStatus getShopStatus(String userId);
    PublicVendorResponse getVendorByCode(String vendorCode);
    ShopStatus getVendorStatusByCode(String vendorCode);
}
