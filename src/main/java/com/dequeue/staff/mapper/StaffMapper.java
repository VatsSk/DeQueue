package com.dequeue.staff.mapper;

import com.dequeue.staff.dto.CreateStaffRequest;
import com.dequeue.staff.dto.StaffResponse;
import com.dequeue.staff.entity.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "departmentName", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    @Mapping(target = "effectivePermissions", ignore = true)
    @Mapping(target = "roleIds", source = "roles")
    StaffResponse toResponse(Staff staff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "vendorId", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "platformAdmin", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", source = "roleIds")
    Staff toEntity(CreateStaffRequest request);
}
