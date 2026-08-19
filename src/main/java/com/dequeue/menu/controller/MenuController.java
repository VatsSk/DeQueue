package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.menu.dto.CreateMenuItemRequest;
import com.dequeue.menu.dto.MenuItemResponse;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateMenuItemRequest;
import com.dequeue.menu.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {
    private final MenuItemService service;

    @GetMapping("/items")
    @PreAuthorize("hasPermission(null, 'menu.view')")
    public ApiResponse<PageResponse<MenuItemResponse>> getItems(@RequestParam(required = false) String categoryId, Pageable pageable) {
        return ApiResponse.success(service.getMenuItems(categoryId, pageable));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasPermission(null, 'menu.view')")
    public ApiResponse<MenuItemResponse> getItem(@PathVariable String id) {
        return ApiResponse.success(service.getMenuItem(id));
    }

    @PostMapping("/items")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<MenuItemResponse> createItem(@Valid @RequestBody CreateMenuItemRequest request) {
        return ApiResponse.success(service.createMenuItem(request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<MenuItemResponse> updateItem(@PathVariable String id, @Valid @RequestBody UpdateMenuItemRequest request) {
        return ApiResponse.success(service.updateMenuItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<Void> deleteItem(@PathVariable String id) {
        service.deleteMenuItem(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/items/{id}/availability")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<MenuItemResponse> toggleAvailability(@PathVariable String id) {
        return ApiResponse.success(service.toggleAvailability(id));
    }

    @PatchMapping("/items/{id}/visibility")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<MenuItemResponse> toggleVisibility(@PathVariable String id) {
        return ApiResponse.success(service.toggleVisibility(id));
    }
    
    @PutMapping("/items/sort")
    @PreAuthorize("hasPermission(null, 'menu.edit')")
    public ApiResponse<Void> updateSort(@RequestBody SortOrderRequest request) {
        service.updateSortOrder(request);
        return ApiResponse.success(null);
    }
}
