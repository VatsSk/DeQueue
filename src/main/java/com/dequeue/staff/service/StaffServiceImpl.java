package com.dequeue.staff.service;

import com.dequeue.common.audit.AuditService;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public PageResponse<StaffResponse> findAll(int page, int size) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Page<Staff> staffPage = staffRepository.findByVendorId(vendorId, PageRequest.of(page, size));
        
        List<StaffResponse> content = staffPage.getContent().stream()
                .map(this::enrichWithDepartmentName)
                .collect(Collectors.toList());
                
        return PageResponse.of(content, staffPage);
    }

    @Override
    public StaffResponse findById(String id) {
        Staff staff = getStaff(id);
        return enrichWithDepartmentName(staff);
    }

    @Override
    public StaffResponse create(CreateStaffRequest request) {
        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        
        String vendorId = SecurityUtils.getCurrentVendorId();
        if (request.getDepartmentId() != null) {
            validateDepartment(request.getDepartmentId(), vendorId);
        }

        Staff staff = staffMapper.toEntity(request);
        staff.setVendorId(vendorId);
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setStatus(StaffStatus.ACTIVE);
        
        staff = staffRepository.save(staff);
        auditService.logAction("CREATE_STAFF", "Staff created: " + staff.getId());
        return enrichWithDepartmentName(staff);
    }

    @Override
    public StaffResponse update(String id, UpdateStaffRequest request) {
        Staff staff = getStaff(id);
        String vendorId = SecurityUtils.getCurrentVendorId();

        if (request.getDepartmentId() != null && !request.getDepartmentId().equals(staff.getDepartmentId())) {
            validateDepartment(request.getDepartmentId(), vendorId);
        }

        staff.setName(request.getName());
        staff.setPhone(request.getPhone());
        staff.setDepartmentId(request.getDepartmentId());
        if (request.getRole() != null) staff.setRole(request.getRole());
        if (request.getPermissions() != null) staff.setPermissions(request.getPermissions());

        staff = staffRepository.save(staff);
        auditService.logAction("UPDATE_STAFF", "Staff updated: " + staff.getId());
        return enrichWithDepartmentName(staff);
    }

    @Override
    public void delete(String id) {
        Staff staff = getStaff(id);
        staff.setStatus(StaffStatus.INACTIVE);
        staffRepository.save(staff);
        auditService.logAction("DELETE_STAFF", "Staff deleted: " + id);
    }

    @Override
    public StaffResponse changeStatus(String id, StaffStatusRequest request) {
        Staff staff = getStaff(id);
        staff.setStatus(request.getStatus());
        staff = staffRepository.save(staff);
        auditService.logAction("CHANGE_STAFF_STATUS", "Staff status changed: " + id);
        return enrichWithDepartmentName(staff);
    }

    @Override
    public List<StaffResponse> findByDepartment(String departmentId) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        return staffRepository.findByVendorIdAndDepartmentId(vendorId, departmentId)
                .stream()
                .map(this::enrichWithDepartmentName)
                .collect(Collectors.toList());
    }

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
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!dept.getVendorId().equals(vendorId)) {
            throw new ResourceNotFoundException("Department not found in your vendor scope");
        }
    }

    private StaffResponse enrichWithDepartmentName(Staff staff) {
        StaffResponse response = staffMapper.toResponse(staff);
        if (staff.getDepartmentId() != null) {
            departmentRepository.findById(staff.getDepartmentId())
                    .ifPresent(d -> response.setDepartmentName(d.getName()));
        }
        return response;
    }
}
