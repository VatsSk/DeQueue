package com.dequeue.cashfree.dto;

import com.dequeue.vendor.entity.CashfreeVendorInfo;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class VendorOnboardingResponse {
    private String vendorId; // DeQueue Mongo vendor ID
    private String cashfreeVendorId;
    private String status;
    private String onboardingStatus;
    private boolean easySplitEnabled;
    private Instant lastSyncedAt;
    private String message;
    /** Masked bank account number (XXXXXX + last 4 digits). */
    private String maskedBankAccount;
}
