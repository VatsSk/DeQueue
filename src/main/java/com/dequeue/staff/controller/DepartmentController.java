package com.dequeue.staff.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.staff.dto.CreateDepartmentRequest;
import com.dequeue.staff.dto.DepartmentResponse;
import com.dequeue.staff.dto.UpdateDepartmentRequest;
import com.dequeue.staff.service.DepartmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department Management APIs")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> getAll() {
        return ApiResponse.success(departmentService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getById(@PathVariable String id) {
        return ApiResponse.success(departmentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGE_STAFF')")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.success(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGE_STAFF')")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGE_STAFF')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        departmentService.delete(id);
        return ApiResponse.success(null);
    }
}
