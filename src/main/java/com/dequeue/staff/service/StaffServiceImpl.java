package com.dequeue.staff.service;

import com.dequeue.common.audit.AuditService;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.rbac.entity.RbacPermission;
import com.dequeue.rbac.entity.RbacRole;
import com.dequeue.rbac.repository.RbacPermissionRepository;
import com.dequeue.rbac.repository.RbacRoleRepository;
import com.dequeue.staff.dto.CreateStaffRequest;
import com.dequeue.staff.dto.StaffResponse;
import com.dequeue.staff.dto.StaffStatusRequest;
import com.dequeue.staff.dto.UpdateStaffRequest;
import com.dequeue.staff.entity.Department;
import com.dequeue.staff.entity.Staff;
import com.dequeue.staff.entity.StaffStatus;
import com.dequeue.staff.mapper.StaffMapper;
import com.dequeue.staff.repository.DepartmentRepository;
import com.dequeue.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final RbacRoleRepository roleRepository;
    private final RbacPermissionRepository permissionRepository;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public PageResponse<StaffResponse> findAll(int page, int size) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        String currentUserId = SecurityUtils.getCurrentUserId();
        Page<Staff> staffPage = staffRepository.findByVendorId(vendorId, PageRequest.of(page, size));

        List<StaffResponse> content = staffPage.getContent().stream()
                .filter(staff -> !staff.getId().equals(currentUserId)) // Exclude current user
                .map(this::enrichWithDetails)
                .collect(Collectors.toList());

        return PageResponse.of(content, staffPage);
    }

    @Override
    public StaffResponse findById(String id) {
        Staff staff = getStaff(id);
        return enrichWithDetails(staff);
    }

    @Override
    public StaffResponse create(CreateStaffRequest request) {
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        String vendorId = SecurityUtils.getCurrentVendorId();

        // Validate departments belong to this vendor
        if (request.getDepartmentIds() != null) {
            request.getDepartmentIds().forEach(deptId -> validateDepartment(deptId, vendorId));
        }

        // Validate roles belong to this vendor
        validateRoleIds(request.getRoleIds(), vendorId);

        Staff staff = staffMapper.toEntity(request);
        staff.setVendorId(vendorId);
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setStatus(StaffStatus.ACTIVE);
        staff.setRoleIds(request.getRoleIds() != null ? request.getRoleIds() : new ArrayList<>());
        staff.setDepartmentIds(request.getDepartmentIds() != null ? request.getDepartmentIds() : new ArrayList<>());

        staff = staffRepository.save(staff);
        auditService.logAction("CREATE_STAFF", "Staff created: " + staff.getId());
        return enrichWithDetails(staff);
    }

    @Override
    public StaffResponse update(String id, UpdateStaffRequest request) {
        Staff staff = getStaff(id);
        String vendorId = SecurityUtils.getCurrentVendorId();

        if (request.getDepartmentIds() != null) {
            request.getDepartmentIds().forEach(deptId -> validateDepartment(deptId, vendorId));
            staff.setDepartmentIds(request.getDepartmentIds());
        }

        if (request.getRoleIds() != null) {
            validateRoleIds(request.getRoleIds(), vendorId);
            staff.setRoleIds(request.getRoleIds());
        }

        staff.setName(request.getName());
        if (request.getPhone() != null) staff.setPhone(request.getPhone());

        staff = staffRepository.save(staff);
        auditService.logAction("UPDATE_STAFF", "Staff updated: " + staff.getId());
        return enrichWithDetails(staff);
    }

    @Override
    public void delete(String id) {
        Staff staff = getStaff(id);
        staff.setStatus(StaffStatus.INACTIVE);
        staffRepository.save(staff);
        auditService.logAction("DELETE_STAFF", "Staff deactivated: " + id);
    }

    @Override
    public StaffResponse changeStatus(String id, StaffStatusRequest request) {
        Staff staff = getStaff(id);
        staff.setStatus(request.getStatus());
        staff = staffRepository.save(staff);
        auditService.logAction("CHANGE_STAFF_STATUS", "Staff status changed: " + id + " → " + request.getStatus());
        return enrichWithDetails(staff);
    }

    @Override
    public List<StaffResponse> findByDepartment(String departmentId) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        return staffRepository.findByVendorIdAndDepartmentIdsContaining(vendorId, departmentId)
                .stream()
                .map(this::enrichWithDetails)
                .collect(Collectors.toList());
    }

    // ────────────────────────── private helpers ──────────────────────────

    private Staff getStaff(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        if (!staff.getVendorId().equals(vendorId)) {
            throw new ResourceNotFoundException("Staff not found in your vendor scope");
        }
        return staff;
    }

    private void validateDepartment(String departmentId, String vendorId) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
        if (!dept.getVendorId().equals(vendorId)) {
            throw new ResourceNotFoundException("Department not found in your vendor scope: " + departmentId);
        }
    }

    private void validateRoleIds(List<String> roleIds, String vendorId) {
        if (roleIds == null || roleIds.isEmpty()) return;

        List<RbacRole> found = roleRepository.findByIdInAndVendorId(roleIds, vendorId);
        if (found.size() != roleIds.size()) {
            List<String> foundIds = found.stream().map(RbacRole::getId).collect(Collectors.toList());
            List<String> invalid = roleIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            throw new BadRequestException("Invalid role IDs (not found or not in your vendor scope): " + invalid);
        }
    }

    /**
     * Enriches a StaffResponse with resolved role names, department name,
     * and effective permissions — all required by the FRD for Android dynamic UI.
     */
    private StaffResponse enrichWithDetails(Staff staff) {
        StaffResponse response = staffMapper.toResponse(staff);

        // Resolve first department name
        if (staff.getDepartmentIds() != null && !staff.getDepartmentIds().isEmpty()) {
            departmentRepository.findById(staff.getDepartmentIds().get(0))
                    .ifPresent(d -> response.setDepartmentName(d.getName()));
        }

        // Resolve role names and effective permissions
        if (staff.getRoleIds() != null && !staff.getRoleIds().isEmpty()) {
            List<RbacRole> roles = roleRepository.findByIdIn(staff.getRoleIds());

            List<String> roleNames = roles.stream()
                    .map(RbacRole::getName)
                    .collect(Collectors.toList());
            response.setRoleNames(roleNames);

            // Collect all permission IDs, deduplicated
            List<String> allPermissionIds = roles.stream()
                    .flatMap(r -> r.getPermissionIds() != null ? r.getPermissionIds().stream() : java.util.stream.Stream.empty())
                    .distinct()
                    .collect(Collectors.toList());

            if (!allPermissionIds.isEmpty()) {
                List<String> permKeys = permissionRepository.findByIdIn(allPermissionIds).stream()
                        .filter(RbacPermission::isActive)
                        .map(RbacPermission::getPermissionKey)
                        .collect(Collectors.toList());
                response.setEffectivePermissions(permKeys);
            }
        }

        return response;
    }
}
