package com.dequeue.auth.dto;

import com.dequeue.staff.entity.Permission;
import com.dequeue.staff.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StaffSummary {
    private String id;
    private String name;
    private String email;
    private Role role;
    private String department;
    private List<Permission> permissions;
    private String vendorId;
    private String vendorCode;
    private String shopName;
}
