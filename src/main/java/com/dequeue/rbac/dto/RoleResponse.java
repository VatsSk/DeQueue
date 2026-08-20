package com.dequeue.rbac.dto;

import com.dequeue.rbac.entity.OrderVisibility;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class RoleResponse {

    private String id;
    private String name;
    private String description;

    /** Raw permission IDs */
    private List<String> permissionIds;

    /** Human-readable permission keys e.g. ["order.accept", "menu.view"] */
    private List<String> permissionKeys;

    private OrderVisibility orderVisibility;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
