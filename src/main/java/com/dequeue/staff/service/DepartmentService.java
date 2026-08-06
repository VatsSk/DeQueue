package com.dequeue.staff.service;

import com.dequeue.staff.dto.CreateDepartmentRequest;
import com.dequeue.staff.dto.DepartmentResponse;
import com.dequeue.staff.dto.UpdateDepartmentRequest;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResponse> findAll();
    DepartmentResponse findById(String id);
    DepartmentResponse create(CreateDepartmentRequest request);
    DepartmentResponse update(String id, UpdateDepartmentRequest request);
    void delete(String id);
}
