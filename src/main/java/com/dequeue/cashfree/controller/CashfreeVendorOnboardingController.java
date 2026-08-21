package com.dequeue.cashfree.controller;

import com.dequeue.cashfree.dto.VendorOnboardingRequest;
import com.dequeue.cashfree.dto.VendorOnboardingResponse;
import com.dequeue.cashfree.service.CashfreeEasySplitService;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.vendor.entity.CashfreeVendorInfo;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/platform/vendors")
@RequiredArgsConstructor
@Tag(name = "Cashfree Vendor Onboarding", description = "Easy Split vendor management — Platform Admin only")
public class CashfreeVendorOnboardingController {

    private final CashfreeEasySplitService easySplitService;
    private final VendorRepository vendorRepository;

    /**
     * Onboard a vendor into Cashfree Easy Split.
     * Creates the vendor in Cashfree and stores the cashfreeVendorId in the Vendor document.
     */
    @PostMapping("/{vendorId}/cashfree/onboard")
    @PreAuthorize("T(com.dequeue.common.security.SecurityUtils).isPlatformAdmin() or T(com.dequeue.common.security.SecurityUtils).getCurrentVendorId() == #vendorId")
    @Operation(summary = "Onboard vendor to Cashfree Easy Split")
    public ApiResponse<VendorOnboardingResponse> onboardVendor(
            @PathVariable String vendorId,
            @Valid @RequestBody VendorOnboardingRequest request) {

        log.info("Initiating Cashfree Easy Split onboarding for vendorId={}", vendorId);

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));

        String cashfreeVendorId = CashfreeEasySplitService.buildCashfreeVendorId(vendorId);

        // Build Cashfree vendor request body
        Map<String, Object> vendorDetails = buildCashfreeVendorPayload(vendor, request);

        Map<String, Object> cfResponse;
        try {
            cfResponse = easySplitService.createVendor(vendorId, vendorDetails);
        } catch (Exception e) {
            log.error("Cashfree vendor onboarding failed for vendorId={}: {}", vendorId, e.getMessage());
            // Update onboarding status to FAILED
            updateVendorCashfreeInfo(vendor, cashfreeVendorId, "PENDING", "FAILED",
                    e.getMessage(), false);
            throw new RuntimeException("Cashfree vendor onboarding failed: " + e.getMessage(), e);
        }

        // Parse response and update vendor
        String status = (String) cfResponse.getOrDefault("status", "PENDING");
        updateVendorCashfreeInfo(vendor, cashfreeVendorId, status, "PENDING", null, true);

        log.info("Cashfree vendor onboarding initiated: vendorId={}, cashfreeVendorId={}, status={}",
                vendorId, cashfreeVendorId, status);

        return ApiResponse.success(VendorOnboardingResponse.builder()
                .vendorId(vendorId)
                .cashfreeVendorId(cashfreeVendorId)
                .status(status)
                .onboardingStatus("PENDING")
                .easySplitEnabled(false) // Active after approval
                .message("Onboarding initiated. KYC approval may take 1-3 business days.")
                .build());
    }

    /**
     * Get Cashfree Easy Split status for a vendor.
     */
    @GetMapping("/{vendorId}/cashfree/status")
    @PreAuthorize("T(com.dequeue.common.security.SecurityUtils).isPlatformAdmin() or T(com.dequeue.common.security.SecurityUtils).getCurrentVendorId() == #vendorId")
    @Operation(summary = "Get vendor Cashfree Easy Split status")
    public ApiResponse<VendorOnboardingResponse> getVendorCashfreeStatus(@PathVariable String vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));

        CashfreeVendorInfo info = vendor.getCashfreeInfo();
        if (info == null) {
            return ApiResponse.success(VendorOnboardingResponse.builder()
                    .vendorId(vendorId)
                    .status("NOT_ONBOARDED")
                    .message("Vendor has not been onboarded to Cashfree Easy Split yet.")
                    .build());
        }

        String maskedBank = maskBankAccount(vendor.getSettings() != null
                ? vendor.getSettings().getBankAccountNumber() : null);

        return ApiResponse.success(VendorOnboardingResponse.builder()
                .vendorId(vendorId)
                .cashfreeVendorId(info.getCashfreeVendorId())
                .status(info.getStatus())
                .onboardingStatus(info.getOnboardingStatus())
                .easySplitEnabled(info.isEasySplitEnabled())
                .lastSyncedAt(info.getLastSyncedAt())
                .maskedBankAccount(maskedBank)
                .build());
    }

    /**
     * Sync vendor status from Cashfree.
     */
    @PostMapping("/{vendorId}/cashfree/sync")
    @PreAuthorize("T(com.dequeue.common.security.SecurityUtils).isPlatformAdmin()")
    @Operation(summary = "Sync vendor Cashfree status from Cashfree API")
    public ApiResponse<VendorOnboardingResponse> syncVendorStatus(@PathVariable String vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));

        if (vendor.getCashfreeInfo() == null || vendor.getCashfreeInfo().getCashfreeVendorId() == null) {
            throw new com.dequeue.common.exception.BadRequestException(
                    "Vendor has not been onboarded to Cashfree yet");
        }

        String cashfreeVendorId = vendor.getCashfreeInfo().getCashfreeVendorId();
        log.info("Syncing Cashfree vendor status: vendorId={}, cashfreeVendorId={}",
                vendorId, cashfreeVendorId);

        try {
            Map<String, Object> cfResponse = easySplitService.getVendor(cashfreeVendorId);
            String status = (String) cfResponse.getOrDefault("status", vendor.getCashfreeInfo().getStatus());
            String onboardingStatus = (String) cfResponse.getOrDefault("kyc_details",
                    Map.of("kyc_status", "PENDING"));

            CashfreeVendorInfo info = vendor.getCashfreeInfo();
            info.setStatus(status);
            info.setLastSyncedAt(Instant.now());
            info.setLastSyncStatus("SUCCESS");
            info.setEasySplitEnabled("ACTIVE".equalsIgnoreCase(status));
            vendor.setCashfreeInfo(info);
            vendorRepository.save(vendor);

            log.info("Vendor {} synced from Cashfree. status={}", vendorId, status);

            return ApiResponse.success(VendorOnboardingResponse.builder()
                    .vendorId(vendorId)
                    .cashfreeVendorId(cashfreeVendorId)
                    .status(status)
                    .easySplitEnabled(info.isEasySplitEnabled())
                    .lastSyncedAt(info.getLastSyncedAt())
                    .message("Status synced from Cashfree")
                    .build());
        } catch (Exception e) {
            log.error("Failed to sync vendor {} from Cashfree: {}", vendorId, e.getMessage());
            CashfreeVendorInfo info = vendor.getCashfreeInfo();
            info.setLastSyncedAt(Instant.now());
            info.setLastSyncStatus("FAILED");
            info.setLastSyncError(e.getMessage());
            vendor.setCashfreeInfo(info);
            vendorRepository.save(vendor);
            throw new RuntimeException("Cashfree sync failed: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Map<String, Object> buildCashfreeVendorPayload(Vendor vendor, VendorOnboardingRequest request) {
        Map<String, Object> payload = new HashMap<>();

        // Basic info
        payload.put("name", request.getName() != null ? request.getName() : vendor.getShopName());
        payload.put("email", request.getEmail() != null ? request.getEmail() : vendor.getEmail());
        payload.put("phone", request.getPhone() != null ? request.getPhone() : vendor.getPhone());
        payload.put("business_type", request.getBusinessType());

        // Bank details — prefer request, fallback to vendor settings
        String bankAcc = request.getBankAccountNumber();
        String ifsc = request.getBankIfscCode();
        String bankName = request.getBankAccountName();
        String upi = request.getUpiId();

        if (bankAcc == null && vendor.getSettings() != null) {
            bankAcc = vendor.getSettings().getBankAccountNumber();
            ifsc = vendor.getSettings().getBankIfscCode();
            bankName = vendor.getSettings().getBankAccountName();
        }
        if (upi == null && vendor.getSettings() != null) {
            upi = vendor.getSettings().getUpiId();
        }

        if (bankAcc != null && !bankAcc.isBlank() && ifsc != null && !ifsc.isBlank()) {
            Map<String, String> bank = new HashMap<>();
            bank.put("account_number", bankAcc);
            bank.put("ifsc", ifsc);
            if (bankName != null) bank.put("account_holder_name", bankName);
            payload.put("bank", bank);
        } else if (upi != null && !upi.isBlank()) {
            payload.put("upi", Map.of("vpa", upi));
        }

        // KYC / address
        if (request.getPan() != null) payload.put("pan", request.getPan());
        if (request.getGstNumber() != null) payload.put("gst", request.getGstNumber());

        payload.put("schedule_option", 1); // Daily settlement
        payload.put("dashboard_access", false);

        return payload;
    }

    private void updateVendorCashfreeInfo(Vendor vendor, String cashfreeVendorId,
                                           String status, String onboardingStatus,
                                           String syncError, boolean easySplitEnabled) {
        CashfreeVendorInfo info = vendor.getCashfreeInfo() != null
                ? vendor.getCashfreeInfo() : new CashfreeVendorInfo();
        info.setCashfreeVendorId(cashfreeVendorId);
        info.setStatus(status);
        info.setOnboardingStatus(onboardingStatus);
        info.setEasySplitEnabled(easySplitEnabled && "ACTIVE".equalsIgnoreCase(status));
        info.setLastSyncedAt(Instant.now());
        info.setLastSyncStatus(syncError == null ? "SUCCESS" : "FAILED");
        info.setLastSyncError(syncError);
        info.setOnboardedAt(info.getOnboardedAt() != null ? info.getOnboardedAt() : Instant.now());
        vendor.setCashfreeInfo(info);
        vendorRepository.save(vendor);
    }

    private String maskBankAccount(String bankAccountNumber) {
        if (bankAccountNumber == null || bankAccountNumber.length() < 4) return null;
        String last4 = bankAccountNumber.substring(bankAccountNumber.length() - 4);
        return "XXXXXX" + last4;
    }
}
