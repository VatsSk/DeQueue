package com.dequeue.vendor.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import com.dequeue.vendor.service.VendorService;
import com.dequeue.vendor.dto.*;
import com.dequeue.vendor.entity.ShopStatus;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.vendor.entity.VendorSettings;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VendorController {

    private final VendorService vendorService;

    @GetMapping("/me")
    public ApiResponse<VendorResponse> getCurrentVendor() {
        return ApiResponse.success(vendorService.getCurrentVendor(SecurityUtils.getCurrentVendorId()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR_ADMIN', 'ROLE_VENDOR_MANAGER') or hasAuthority('ROLE_PLATFORM_ADMIN')")
    @PutMapping("/me")
    public ApiResponse<VendorResponse> updateVendor(@Valid @RequestBody UpdateVendorRequest request) {
        return ApiResponse.success(vendorService.updateVendor(SecurityUtils.getCurrentVendorId(), request));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR_ADMIN', 'ROLE_VENDOR_MANAGER') or hasAuthority('ROLE_PLATFORM_ADMIN')")
    @PatchMapping("/me/status")
    public ApiResponse<ShopStatus> updateShopStatus(@Valid @RequestBody ShopStatusRequest request) {
        return ApiResponse.success(vendorService.updateShopStatus(SecurityUtils.getCurrentVendorId(), request));
    }

    @GetMapping("/me/status")
    public ApiResponse<ShopStatus> getShopStatus() {
        return ApiResponse.success(vendorService.getShopStatus(SecurityUtils.getCurrentVendorId()));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR_ADMIN', 'ROLE_VENDOR_MANAGER') or hasAuthority('ROLE_PLATFORM_ADMIN')")
    @PatchMapping("/me/settings")
    public ApiResponse<VendorSettings> updateSettings(@RequestBody VendorSettingsDto request) {
        return ApiResponse.success(vendorService.updateSettings(SecurityUtils.getCurrentVendorId(), request));
    }
}
