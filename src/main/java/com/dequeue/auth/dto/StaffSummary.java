package com.dequeue.auth.dto;

import com.dequeue.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StaffSummary {
    private String id;
    private String name;
    private String email;

    /** IDs of assigned roles */
    private List<String> roleIds;

    /** Resolved role names */
    private List<String> roleNames;

    /**
     * Effective permission keys from all assigned roles.
     * e.g. ["order.view", "order.accept", "menu.view"]
     * Used by Android client for dynamic UI rendering.
     */
    private List<String> effectivePermissions;

    /**
     * Order status values this user is allowed to see.
     * Used by Android client to filter the orders queue.
     */
    private List<OrderStatus> orderVisibilityStatuses;

    private List<String> departmentIds;
    private String vendorId;
    private String vendorCode;
    private String shopName;
    private boolean platformAdmin;
}
