package com.dequeue.menu.mapper;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import com.dequeue.menu.entity.CustomizationGroup;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomizationGroupMapper {
    CustomizationGroup toEntity(CreateCustomizationGroupRequest request);
    void updateEntity(UpdateCustomizationGroupRequest request, @MappingTarget CustomizationGroup entity);
    CustomizationGroupResponse toResponse(CustomizationGroup entity);
}
