package com.dequeue.profile.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import com.dequeue.profile.service.ProfileService;
import com.dequeue.profile.dto.*;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.security.SecurityUtils;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping({"", "/"})
    @PreAuthorize("hasPermission(null, 'staff.view')")
    public ApiResponse<ProfileResponse> getProfile() {
        return ApiResponse.success(profileService.getProfile(SecurityUtils.getCurrentVendorId()));
    }

    @PutMapping({"", "/"})
    @PreAuthorize("hasPermission(null, 'staff.edit')")
    public ApiResponse<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(profileService.updateProfile(SecurityUtils.getCurrentVendorId(), request));
    }

    @PatchMapping("/logo")
    @PreAuthorize("hasPermission(null, 'staff.edit')")
    public ApiResponse<ProfileResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(profileService.uploadLogo(SecurityUtils.getCurrentVendorId(), file));
    }

    @PatchMapping("/banner")
    @PreAuthorize("hasPermission(null, 'staff.edit')")
    public ApiResponse<ProfileResponse> uploadBanner(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(profileService.uploadBanner(SecurityUtils.getCurrentVendorId(), file));
    }
}
