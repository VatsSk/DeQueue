package com.dequeue.rbac.service;

import com.dequeue.rbac.dto.CreateRoleRequest;
import com.dequeue.rbac.dto.RoleResponse;
import com.dequeue.rbac.dto.UpdateRoleRequest;

import java.util.List;

public interface RoleService {
    List<RoleResponse> findAll();
    RoleResponse findById(String id);
    RoleResponse create(CreateRoleRequest request);
    RoleResponse update(String id, UpdateRoleRequest request);
    void delete(String id);
}
