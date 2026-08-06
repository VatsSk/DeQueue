package com.dequeue.staff.mapper;

import com.dequeue.staff.dto.CreateStaffRequest;
import com.dequeue.staff.dto.StaffResponse;
import com.dequeue.staff.entity.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffMapper {
    @Mapping(target = "departmentName", ignore = true)
    StaffResponse toResponse(Staff staff);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "vendorId", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    Staff toEntity(CreateStaffRequest request);
}
