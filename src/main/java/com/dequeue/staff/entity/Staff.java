package com.dequeue.staff.entity;

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
@Document(collection = "staff")
public class Staff {

    @Id
    private String id;

    @Indexed
    private String vendorId;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String phone;

    /** Static roles assigned to this staff member (e.g. ROLE_VENDOR_ADMIN). */
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    /** IDs of Department documents this staff belongs to. */
    @Builder.Default
    private List<String> departmentIds = new ArrayList<>();

    private StaffStatus status;
    private String avatar;
    private Instant lastLoginAt;

    /**
     * When true, this staff member is a DeQueue Platform Admin and operates
     * independently of vendor-specific roles. Platform Admins can manage vendors
     * and the global permission catalog.
     */
    @Builder.Default
    private boolean platformAdmin = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
