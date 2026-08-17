package com.dequeue.rbac.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permissions")
@CompoundIndex(name = "resource_action_unique", def = "{'resource': 1, 'action': 1}", unique = true)
public class RbacPermission {

    @Id
    private String id;

    /** e.g. "order", "menu", "staff", "role", "report" */
    private String resource;

    /** e.g. "view", "accept", "prepare", "create", "delete" */
    private String action;

    private String description;

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** Computed full key: resource + "." + action  →  "order.accept" */
    public String getPermissionKey() {
        return resource + "." + action;
    }
}
