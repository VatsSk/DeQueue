package com.dequeue.settings.dto;

import lombok.Data;

@Data
public class UpdateOrderSettingsRequest {
    private boolean allowCustomOrder;
    private boolean requireApproval;
    private int maxOrdersPerCustomer;
}
