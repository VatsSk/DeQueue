package com.dequeue.staff.service;

import com.dequeue.common.dto.PageResponse;
import com.dequeue.staff.dto.CreateStaffRequest;
import com.dequeue.staff.dto.StaffResponse;
import com.dequeue.staff.dto.StaffStatusRequest;
import com.dequeue.staff.dto.UpdateStaffRequest;

import java.util.List;

public interface StaffService {
    PageResponse<StaffResponse> findAll(int page, int size);
    StaffResponse findById(String id);
    StaffResponse create(CreateStaffRequest request);
    StaffResponse update(String id, UpdateStaffRequest request);
    void delete(String id);
    StaffResponse changeStatus(String id, StaffStatusRequest request);
    List<StaffResponse> findByDepartment(String departmentId);
}
