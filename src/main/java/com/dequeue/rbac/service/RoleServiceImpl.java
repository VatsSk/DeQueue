package com.dequeue.rbac.service;

import com.dequeue.common.audit.AuditService;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.rbac.dto.CreateRoleRequest;
import com.dequeue.rbac.dto.RoleResponse;
import com.dequeue.rbac.dto.UpdateRoleRequest;
import com.dequeue.rbac.entity.OrderVisibility;
import com.dequeue.rbac.entity.RbacPermission;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.rbac.repository.RbacPermissionRepository;
import com.dequeue.rbac.repository.RbacRoleRepository;
import com.dequeue.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RbacRoleRepository roleRepository;
    private final RbacPermissionRepository permissionRepository;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    @Override
    public List<RoleResponse> findAll() {
        String vendorId = SecurityUtils.getCurrentVendorId();
        List<RbacRole> roles = roleRepository.findByVendorId(vendorId);
        return roles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public RoleResponse findById(String id) {
        return toResponse(getRole(id));
    }

    @Override
    public RoleResponse create(CreateRoleRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();

        validatePermissionIds(request.getPermissionIds());

        RbacRole role = RbacRole.builder()
                .vendorId(vendorId)
                .name(request.getName())
                .description(request.getDescription())
                .permissionIds(request.getPermissionIds())
                .orderVisibility(request.getOrderVisibility() != null
                        ? request.getOrderVisibility()
                        : new OrderVisibility())
                .active(true)
                .build();

        role = roleRepository.save(role);
        auditService.logAction("CREATE_ROLE", "Role created: " + role.getId() + " name=" + role.getName());
        log.info("Role created: {} for vendor: {}", role.getName(), vendorId);
        return toResponse(role);
    }

    @Override
    public RoleResponse update(String id, UpdateRoleRequest request) {
        RbacRole role = getRole(id);

        if (request.getPermissionIds() != null) {
            validatePermissionIds(request.getPermissionIds());
            role.setPermissionIds(request.getPermissionIds());
        }

        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        if (request.getOrderVisibility() != null) role.setOrderVisibility(request.getOrderVisibility());
        if (request.getActive() != null) role.setActive(request.getActive());

        role = roleRepository.save(role);
        auditService.logAction("UPDATE_ROLE", "Role updated: " + role.getId());
        return toResponse(role);
    }

    @Override
    public void delete(String id) {
        RbacRole role = getRole(id);
        String vendorId = SecurityUtils.getCurrentVendorId();

        // Check if any staff members are still assigned this role
        long staffCount = staffRepository.findByVendorId(vendorId).stream()
                .filter(s -> s.getRoleIds() != null && s.getRoleIds().contains(id))
                .count();

        if (staffCount > 0) {
            throw new BadRequestException(
                    "Cannot delete role '" + role.getName() + "' — it is assigned to " + staffCount + " staff member(s). " +
                    "Remove the role from all staff before deleting.");
        }

        roleRepository.delete(role);
        auditService.logAction("DELETE_ROLE", "Role deleted: " + id + " name=" + role.getName());
    }

    private RbacRole getRole(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        RbacRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (!role.getVendorId().equals(vendorId)) {
            throw new ResourceNotFoundException("Role not found in your vendor scope");
        }
        return role;
    }

    private void validatePermissionIds(List<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return;

        List<RbacPermission> found = permissionRepository.findByIdIn(permissionIds);
        Set<String> foundIds = found.stream().map(RbacPermission::getId).collect(Collectors.toSet());

        List<String> invalid = permissionIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        if (!invalid.isEmpty()) {
            throw new BadRequestException("Invalid permission IDs (not in platform catalog): " + invalid);
        }

        List<String> inactive = found.stream()
                .filter(p -> !p.isActive())
                .map(RbacPermission::getId)
                .collect(Collectors.toList());

        if (!inactive.isEmpty()) {
            throw new BadRequestException("Cannot assign inactive permissions: " + inactive);
        }
    }

    private RoleResponse toResponse(RbacRole role) {
        List<String> permissionKeys = new ArrayList<>();
        if (role.getPermissionIds() != null && !role.getPermissionIds().isEmpty()) {
            List<RbacPermission> permissions = permissionRepository.findByIdIn(role.getPermissionIds());
            permissionKeys = permissions.stream()
                    .map(RbacPermission::getPermissionKey)
                    .collect(Collectors.toList());
        }

        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setVendorId(role.getVendorId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setPermissionIds(role.getPermissionIds());
        response.setPermissionKeys(permissionKeys);
        response.setOrderVisibility(role.getOrderVisibility());
        response.setActive(role.isActive());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }
}
