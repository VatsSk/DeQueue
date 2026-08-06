package com.dequeue.staff.dto;

import com.dequeue.staff.entity.Permission;
import com.dequeue.staff.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateStaffRequest {
    @NotBlank
    private String name;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String password;
    private String phone;
    private String departmentId;
    @NotNull
    private Role role;
    private List<Permission> permissions;
}
