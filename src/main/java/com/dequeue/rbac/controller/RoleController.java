package com.dequeue.rbac.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.rbac.dto.CreateRoleRequest;
import com.dequeue.rbac.dto.RoleResponse;
import com.dequeue.rbac.dto.UpdateRoleRequest;
import com.dequeue.rbac.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Vendor Role Management APIs")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'staff.view')")
    public ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.success(roleService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'role.view')")
    public ApiResponse<RoleResponse> getById(@PathVariable String id) {
        return ApiResponse.success(roleService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'role.create')")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'role.update')")
    public ApiResponse<RoleResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'role.delete')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ApiResponse.success(null);
    }
}
