package com.dequeue.staff.dto;

import com.dequeue.staff.entity.Permission;
import com.dequeue.staff.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateStaffRequest {
    @NotBlank
    private String name;
    private String phone;
    private String departmentId;
    private Role role;
    private List<Permission> permissions;
}
