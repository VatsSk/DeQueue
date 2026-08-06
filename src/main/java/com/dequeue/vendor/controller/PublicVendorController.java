package com.dequeue.vendor.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.dequeue.vendor.service.VendorService;
import com.dequeue.vendor.dto.*;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/public/vendors")
@RequiredArgsConstructor
public class PublicVendorController {

    private final VendorService vendorService;

    @GetMapping("/{vendorCode}")
    public ApiResponse<PublicVendorResponse> getVendorByCode(@PathVariable String vendorCode) {
        return ApiResponse.success(vendorService.getVendorByCode(vendorCode));
    }

    @GetMapping("/{vendorCode}/status")
    public ApiResponse<ShopStatus> getVendorStatus(@PathVariable String vendorCode) {
        return ApiResponse.success(vendorService.getVendorStatusByCode(vendorCode));
    }
}
