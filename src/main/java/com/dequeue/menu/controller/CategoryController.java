package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.CategoryResponse;
import com.dequeue.menu.dto.CreateCategoryRequest;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateCategoryRequest;
import com.dequeue.menu.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'menu.view')")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(service.getCategories());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'menu.view')")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable String id) {
        return ApiResponse.success(service.getCategory(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(service.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(service.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<Void> deleteCategory(@PathVariable String id) {
        service.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/sort")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<Void> updateSort(@RequestBody SortOrderRequest request) {
        service.updateSortOrder(request);
        return ApiResponse.success(null);
    }
}
