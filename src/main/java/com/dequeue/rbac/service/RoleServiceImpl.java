package com.dequeue.rbac.service;

import com.dequeue.common.audit.AuditService;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
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
        return roleRepository.findByActiveTrue().stream()
                .filter(role -> !role.getName().equalsIgnoreCase("ROLE_VENDOR_ADMIN"))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoleResponse findById(String id) {
        return toResponse(getRole(id));
    }

    @Override
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BadRequestException("A role with name '" + request.getName() + "' already exists");
        }

        RbacRole role = RbacRole.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(request.getPermissionIds() != null ? request.getPermissionIds() : new ArrayList<>())
                .orderVisibility(request.getOrderVisibility() != null
                        ? request.getOrderVisibility()
                        : new OrderVisibility())
                .active(true)
                .build();

        role = roleRepository.save(role);
        auditService.logAction("CREATE_ROLE", "Role created: " + role.getId() + " name=" + role.getName());
        log.info("Global role created: {}", role.getName());
        return toResponse(role);
    }

    @Override
    public RoleResponse update(String id, UpdateRoleRequest request) {
        RbacRole role = getRole(id);

        if (request.getPermissionIds() != null) {
            role.setPermissions(request.getPermissionIds());
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

        // Check if any staff members are still assigned this role ID
        long staffCount = staffRepository.findAll().stream()
                .filter(s -> s.getRoles() != null && s.getRoles().contains(id))
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
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    private RoleResponse toResponse(RbacRole role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setDescription(role.getDescription());
        response.setPermissionIds(role.getPermissions() != null ? role.getPermissions() : new ArrayList<>());
        response.setPermissionKeys(role.getPermissions() != null ? role.getPermissions() : new ArrayList<>());
        response.setOrderVisibility(role.getOrderVisibility());
        response.setActive(role.isActive());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }
}
