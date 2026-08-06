package com.dequeue.menu.service;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.menu.dto.CreateMenuItemRequest;
import com.dequeue.menu.dto.MenuItemResponse;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateMenuItemRequest;
import org.springframework.data.domain.Pageable;

public interface MenuItemService {
    PageResponse<MenuItemResponse> getMenuItems(String categoryId, Pageable pageable);
    MenuItemResponse getMenuItem(String id);
    MenuItemResponse createMenuItem(CreateMenuItemRequest request);
    MenuItemResponse updateMenuItem(String id, UpdateMenuItemRequest request);
    void deleteMenuItem(String id);
    MenuItemResponse toggleAvailability(String id);
    MenuItemResponse toggleVisibility(String id);
    void updateSortOrder(SortOrderRequest request);
}
