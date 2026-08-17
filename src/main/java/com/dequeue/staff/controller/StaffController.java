package com.dequeue.staff.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.staff.dto.CreateStaffRequest;
import com.dequeue.staff.dto.StaffResponse;
import com.dequeue.staff.dto.StaffStatusRequest;
import com.dequeue.staff.dto.UpdateStaffRequest;
import com.dequeue.staff.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Staff Management APIs")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'staff.view')")
    public ApiResponse<PageResponse<StaffResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(staffService.findAll(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'staff.view')")
    public ApiResponse<StaffResponse> getById(@PathVariable String id) {
        return ApiResponse.success(staffService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'staff.create')")
    public ApiResponse<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.success(staffService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'staff.update')")
    public ApiResponse<StaffResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateStaffRequest request) {
        return ApiResponse.success(staffService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'staff.delete')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        staffService.delete(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'staff.update')")
    public ApiResponse<StaffResponse> changeStatus(
            @PathVariable String id,
            @Valid @RequestBody StaffStatusRequest request) {
        return ApiResponse.success(staffService.changeStatus(id, request));
    }

    @GetMapping("/departments/{departmentId}")
    @PreAuthorize("hasPermission(null, 'staff.view')")
    public ApiResponse<List<StaffResponse>> getByDepartment(@PathVariable String departmentId) {
        return ApiResponse.success(staffService.findByDepartment(departmentId));
    }
}
