package com.dequeue.staff.dto;

import com.dequeue.staff.entity.StaffStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class StaffResponse {

    private String id;
    private String name;
    private String email;
    private String phone;

    /** Department IDs */
    private List<String> departmentIds;

    /** For convenience, first department name resolved */
    private String departmentName;

    /** IDs of assigned RbacRole documents */
    private List<String> roleIds;

    /** Resolved role names for display */
    private List<String> roleNames;

    /** Effective permission keys derived from all assigned roles */
    private List<String> effectivePermissions;

    private StaffStatus status;
    private String avatar;
    private boolean platformAdmin;
    private Instant lastLoginAt;
    private Instant createdAt;
}
