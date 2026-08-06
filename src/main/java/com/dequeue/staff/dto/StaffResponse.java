package com.dequeue.staff.dto;

import com.dequeue.staff.entity.Permission;
import com.dequeue.staff.entity.Role;
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
    private String departmentId;
    private String departmentName;
    private Role role;
    private List<Permission> permissions;
    private StaffStatus status;
    private String avatar;
    private Instant lastLoginAt;
    private Instant createdAt;
}
