package com.dequeue.vendor.controller;

import com.dequeue.auth.dto.RegisterRequest;
import com.dequeue.auth.service.AuthService;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.vendor.dto.VendorResponse;
import com.dequeue.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/vendors")
@RequiredArgsConstructor
@PreAuthorize("T(com.dequeue.common.security.SecurityUtils).isPlatformAdmin()")
@Tag(name = "Platform Admin", description = "Platform Administration APIs")
public class PlatformVendorController {

    private final VendorService vendorService;
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List all vendors")
    public ApiResponse<List<VendorResponse>> getAllVendors() {
        return ApiResponse.success(vendorService.getAllVendors());
    }

    @PostMapping
    @Operation(summary = "Create a new vendor (with initial admin user)")
    public ApiResponse<VendorResponse> createVendor(@Valid @RequestBody RegisterRequest request) {
        // Reuse the authService register flow to properly create the Vendor,
        // the initial Vendor Admin role, and the Staff user all at once.
        authService.register(request);
        
        // After creation, we fetch the newly created vendor.
        // We know the email was just registered, so we could theoretically fetch it,
        // but for simplicity we'll just return the updated list or a success message.
        // In a more robust system we'd extract the creation logic.
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a vendor")
    public ApiResponse<VendorResponse> activateVendor(@PathVariable String id) {
        return ApiResponse.success(vendorService.toggleVendorStatus(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a vendor")
    public ApiResponse<VendorResponse> deactivateVendor(@PathVariable String id) {
        return ApiResponse.success(vendorService.toggleVendorStatus(id, false));
    }
}
