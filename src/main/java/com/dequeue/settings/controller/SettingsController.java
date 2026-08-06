package com.dequeue.settings.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import com.dequeue.settings.service.SettingsService;
import com.dequeue.settings.dto.*;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/")
    public ApiResponse<SettingsResponse> getSettings() {
        return ApiResponse.success(settingsService.getSettings(SecurityUtils.getCurrentVendorId()));
    }

    @PutMapping("/")
    public ApiResponse<SettingsResponse> updateAllSettings(@Valid @RequestBody SettingsResponse request) {
        return ApiResponse.success(settingsService.updateAllSettings(SecurityUtils.getCurrentVendorId(), request));
    }

    @PatchMapping("/orders")
    public ApiResponse<SettingsResponse> updateOrderSettings(@Valid @RequestBody UpdateOrderSettingsRequest request) {
        return ApiResponse.success(settingsService.updateOrderSettings(SecurityUtils.getCurrentVendorId(), request));
    }

    @PatchMapping("/queue")
    public ApiResponse<SettingsResponse> updateQueueSettings(@Valid @RequestBody UpdateQueueSettingsRequest request) {
        return ApiResponse.success(settingsService.updateQueueSettings(SecurityUtils.getCurrentVendorId(), request));
    }

    @PatchMapping("/notifications")
    public ApiResponse<SettingsResponse> updateNotificationSettings(@Valid @RequestBody UpdateNotificationSettingsRequest request) {
        return ApiResponse.success(settingsService.updateNotificationSettings(SecurityUtils.getCurrentVendorId(), request));
    }

    @PatchMapping("/display")
    public ApiResponse<SettingsResponse> updateDisplaySettings(@Valid @RequestBody UpdateDisplaySettingsRequest request) {
        return ApiResponse.success(settingsService.updateDisplaySettings(SecurityUtils.getCurrentVendorId(), request));
    }
}
