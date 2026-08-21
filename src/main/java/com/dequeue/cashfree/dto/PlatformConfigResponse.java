package com.dequeue.cashfree.dto;

import com.dequeue.cashfree.entity.CommissionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlatformConfigResponse {
    private CommissionType commissionType;
    private BigDecimal commissionRate;
    private boolean cashfreeEnabled;
    private boolean easySplitEnabled;
    private boolean webhookConfigured;
    private String environment;
}
