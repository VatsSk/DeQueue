package com.dequeue.menu.service;
import com.dequeue.menu.dto.CategoryResponse;
import com.dequeue.menu.dto.CreateCategoryRequest;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateCategoryRequest;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getCategories();
    CategoryResponse getCategory(String id);
    CategoryResponse createCategory(CreateCategoryRequest request);
    CategoryResponse updateCategory(String id, UpdateCategoryRequest request);
    void deleteCategory(String id);
    void updateSortOrder(SortOrderRequest request);
}
