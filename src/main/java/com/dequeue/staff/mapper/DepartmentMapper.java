package com.dequeue.staff.mapper;

import com.dequeue.staff.dto.CreateDepartmentRequest;
import com.dequeue.staff.dto.DepartmentResponse;
import com.dequeue.staff.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    @Mapping(target = "staffCount", ignore = true)
    DepartmentResponse toResponse(Department department);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vendorId", ignore = true)
    @Mapping(target = "active", ignore = true)
    Department toEntity(CreateDepartmentRequest request);
}
