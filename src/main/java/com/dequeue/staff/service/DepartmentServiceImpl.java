package com.dequeue.staff.service;

import com.dequeue.common.audit.AuditService;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.staff.dto.CreateDepartmentRequest;
import com.dequeue.staff.dto.DepartmentResponse;
import com.dequeue.staff.dto.UpdateDepartmentRequest;
import com.dequeue.staff.entity.Department;
import com.dequeue.staff.mapper.DepartmentMapper;
import com.dequeue.staff.repository.DepartmentRepository;
import com.dequeue.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final StaffRepository staffRepository;
    private final DepartmentMapper departmentMapper;
    private final AuditService auditService;

    @Override
    public List<DepartmentResponse> findAll() {
        String vendorId = SecurityUtils.getCurrentVendorId();
        return departmentRepository.findByVendorId(vendorId).stream()
                .map(this::enrichWithStaffCount)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentResponse findById(String id) {
        return enrichWithStaffCount(getDepartment(id));
    }

    @Override
    public DepartmentResponse create(CreateDepartmentRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        
        if (departmentRepository.findByVendorIdAndName(vendorId, request.getName()).isPresent()) {
            throw new BadRequestException("Department with this name already exists");
        }

        Department department = departmentMapper.toEntity(request);
        department.setVendorId(vendorId);
        department.setActive(true);
        
        department = departmentRepository.save(department);
        auditService.logAction("CREATE_DEPARTMENT", "Department created: " + department.getId());
        return enrichWithStaffCount(department);
    }

    @Override
    public DepartmentResponse update(String id, UpdateDepartmentRequest request) {
        Department department = getDepartment(id);
        
        if (!department.getName().equals(request.getName())) {
            String vendorId = SecurityUtils.getCurrentVendorId();
            if (departmentRepository.findByVendorIdAndName(vendorId, request.getName()).isPresent()) {
                throw new BadRequestException("Department with this name already exists");
            }
        }

        department.setName(request.getName());
        department.setDescription(request.getDescription());
        
        department = departmentRepository.save(department);
        auditService.logAction("UPDATE_DEPARTMENT", "Department updated: " + department.getId());
        return enrichWithStaffCount(department);
    }

    @Override
    public void delete(String id) {
        Department department = getDepartment(id);
        
        long staffCount = staffRepository.countByVendorIdAndDepartmentIdsContaining(
                SecurityUtils.getCurrentVendorId(), id);
                
        if (staffCount > 0) {
            throw new BadRequestException("Cannot delete department with active staff");
        }
        
        departmentRepository.delete(department);
        auditService.logAction("DELETE_DEPARTMENT", "Department deleted: " + id);
    }

    private Department getDepartment(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!department.getVendorId().equals(vendorId)) {
            throw new ResourceNotFoundException("Department not found in your vendor scope");
        }
        return department;
    }

    private DepartmentResponse enrichWithStaffCount(Department department) {
        DepartmentResponse response = departmentMapper.toResponse(department);
        long count = staffRepository.countByVendorIdAndDepartmentIdsContaining(
                department.getVendorId(), department.getId());
        response.setStaffCount((int) count);
        return response;
    }
}
