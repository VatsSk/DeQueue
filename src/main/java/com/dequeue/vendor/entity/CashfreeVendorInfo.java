package com.dequeue.vendor.entity;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashfreeVendorInfo {
    /** Deterministic vendor ID used in Cashfree Easy Split: "DEQUEUE_<vendorMongoId>". */
    private String cashfreeVendorId;
    
    /** Current status as reported by Cashfree: ACTIVE, PENDING, SUSPENDED, REJECTED, BANK_ACCOUNT_NOT_ADDED, etc. */
    private String status;
    
    /** Onboarding approval status: APPROVED, PENDING, REJECTED. */
    private String onboardingStatus;
    
    /** Whether this vendor has Cashfree Easy Split dashboard access. */
    private boolean dashboardAccess;
    
    /** Cashfree settlement schedule option (1=daily, 2=weekly, etc.) */
    private Integer scheduleOption;
    
    /** Last time we synced status from Cashfree. */
    private Instant lastSyncedAt;
    
    /** Status of last sync: SUCCESS, FAILED. */
    private String lastSyncStatus;
    
    /** Error message from last failed sync. */
    private String lastSyncError;
    
    /** When this vendor was first created in Cashfree. */
    private Instant onboardedAt;
    
    /** Whether Easy Split is currently enabled for this vendor. */
    private boolean easySplitEnabled;
}
