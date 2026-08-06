package com.dequeue.geofence.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import com.dequeue.geofence.service.GeofenceService;
import com.dequeue.geofence.dto.*;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/geofence")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/")
    public ApiResponse<GeofenceResponse> getGeofenceSettings() {
        return ApiResponse.success(geofenceService.getGeofenceSettings(SecurityUtils.getCurrentVendorId()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/")
    public ApiResponse<GeofenceResponse> updateGeofenceSettings(@Valid @RequestBody UpdateGeofenceRequest request) {
        return ApiResponse.success(geofenceService.updateGeofenceSettings(SecurityUtils.getCurrentVendorId(), request));
    }

    @PostMapping("/validate")
    public ApiResponse<ValidateLocationResponse> validateLocation(@Valid @RequestBody ValidateLocationRequest request) {
        return ApiResponse.success(geofenceService.validateLocation(request));
    }
}
