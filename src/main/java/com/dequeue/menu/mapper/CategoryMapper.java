package com.dequeue.menu.mapper;
import com.dequeue.menu.dto.CategoryResponse;
import com.dequeue.menu.dto.CategoryWithItemsResponse;
import com.dequeue.menu.dto.CreateCategoryRequest;
import com.dequeue.menu.dto.UpdateCategoryRequest;
import com.dequeue.menu.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {
    Category toEntity(CreateCategoryRequest request);
    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category entity);
    CategoryResponse toResponse(Category entity);
    CategoryWithItemsResponse toWithItemsResponse(Category entity);
}
