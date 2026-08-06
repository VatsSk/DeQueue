package com.dequeue.menu.mapper;
import com.dequeue.menu.dto.CreateMenuItemRequest;
import com.dequeue.menu.dto.MenuItemResponse;
import com.dequeue.menu.dto.UpdateMenuItemRequest;
import com.dequeue.menu.entity.MenuItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuItemMapper {
    MenuItem toEntity(CreateMenuItemRequest request);
    void updateEntity(UpdateMenuItemRequest request, @MappingTarget MenuItem entity);
    
    @Mapping(target = "customizationGroups", ignore = true)
    MenuItemResponse toResponse(MenuItem entity);
}
