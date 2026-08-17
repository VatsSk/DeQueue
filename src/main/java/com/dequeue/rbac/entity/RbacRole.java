package com.dequeue.rbac.entity;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class RbacRole {

    @Id
    private String id;

    /** Tenant scope — all roles are vendor-specific */
    @Indexed
    private String vendorId;

    private String name;
    private String description;

    /** References to RbacPermission.id */
    @Builder.Default
    private List<String> permissionIds = new ArrayList<>();

    /** Controls which order statuses this role can see */
    @Builder.Default
    private OrderVisibility orderVisibility = new OrderVisibility();

    @Builder.Default
    private boolean active = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
