package com.dequeue.rbac.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.rbac.dto.CreatePermissionRequest;
import com.dequeue.rbac.dto.PermissionResponse;
import com.dequeue.rbac.entity.RbacPermission;
import com.dequeue.rbac.repository.RbacPermissionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/platform/permissions")
@RequiredArgsConstructor
@Tag(name = "Platform Permissions", description = "Platform-level permission catalog (Platform Admin only for writes)")
public class PermissionController {

    private final RbacPermissionRepository permissionRepository;

    /**
     * Any authenticated user can read the permission catalog.
     * Vendors need this to know what permissions are available when creating roles.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PermissionResponse>> getAll() {
        List<RbacPermission> permissions = permissionRepository.findByActiveTrue();
        return ApiResponse.success(permissions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
        RbacPermission permission = RbacPermission.builder()
                .resource(request.getResource().toLowerCase())
                .action(request.getAction().toLowerCase())
                .description(request.getDescription())
                .active(true)
                .build();
        permission = permissionRepository.save(permission);
        return ApiResponse.success(toResponse(permission));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PermissionResponse> deactivate(@PathVariable String id) {
        RbacPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new com.dequeue.common.exception.ResourceNotFoundException("Permission not found"));
        permission.setActive(false);
        permission = permissionRepository.save(permission);
        return ApiResponse.success(toResponse(permission));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PermissionResponse> activate(@PathVariable String id) {
        RbacPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new com.dequeue.common.exception.ResourceNotFoundException("Permission not found"));
        permission.setActive(true);
        permission = permissionRepository.save(permission);
        return ApiResponse.success(toResponse(permission));
    }

    private PermissionResponse toResponse(RbacPermission p) {
        PermissionResponse response = new PermissionResponse();
        response.setId(p.getId());
        response.setResource(p.getResource());
        response.setAction(p.getAction());
        response.setPermissionKey(p.getPermissionKey());
        response.setDescription(p.getDescription());
        response.setActive(p.isActive());
        response.setCreatedAt(p.getCreatedAt());
        return response;
    }
}
