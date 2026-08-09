package com.dequeue.menu.service;
import com.dequeue.common.dto.PageResponse;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.menu.dto.CreateMenuItemRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.MenuItemResponse;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateMenuItemRequest;
import com.dequeue.menu.entity.MenuItem;
import com.dequeue.menu.mapper.CustomizationGroupMapper;
import com.dequeue.menu.mapper.MenuItemMapper;
import com.dequeue.menu.repository.CategoryRepository;
import com.dequeue.menu.repository.CustomizationGroupRepository;
import com.dequeue.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final CustomizationGroupRepository customizationGroupRepository;
    private final MenuItemMapper menuItemMapper;
    private final CustomizationGroupMapper customizationGroupMapper;

    @Override
    public PageResponse<MenuItemResponse> getMenuItems(String categoryId, Pageable pageable) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Page<MenuItem> page;
        if (categoryId != null) {
            page = menuItemRepository.findByVendorIdAndCategoryId(vendorId, categoryId, pageable);
        } else {
            page = menuItemRepository.findByVendorId(vendorId, pageable);
        }
        return new PageResponse<>(
                page.getContent().stream().map(this::mapToResponseWithGroups).collect(Collectors.toList()),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast()
        );
    }

    @Override
    public MenuItemResponse getMenuItem(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        return mapToResponseWithGroups(item);
    }

    @Override
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        categoryRepository.findByIdAndVendorId(request.getCategoryId(), vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        MenuItem item = menuItemMapper.toEntity(request);
        item.setVendorId(vendorId);
        item.setAvailable(true);
        item.setVisible(true);
        return mapToResponseWithGroups(menuItemRepository.save(item));
    }

    @Override
    public MenuItemResponse updateMenuItem(String id, UpdateMenuItemRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        
        if (request.getCategoryId() != null) {
            categoryRepository.findByIdAndVendorId(request.getCategoryId(), vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }
        
        menuItemMapper.updateEntity(request, item);
        return mapToResponseWithGroups(menuItemRepository.save(item));
    }

    @Override
    public void deleteMenuItem(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        menuItemRepository.delete(item);
    }

    @Override
    public MenuItemResponse toggleAvailability(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        item.setAvailable(!item.isAvailable());
        return mapToResponseWithGroups(menuItemRepository.save(item));
    }

    @Override
    public MenuItemResponse toggleVisibility(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        item.setVisible(!item.isVisible());
        return mapToResponseWithGroups(menuItemRepository.save(item));
    }

    @Override
    public void updateSortOrder(SortOrderRequest request) {
        // Implementation similar to Category sort order update
    }

    private MenuItemResponse mapToResponseWithGroups(MenuItem item) {
        MenuItemResponse resp = menuItemMapper.toResponse(item);
        if (item.getCustomizationGroupIds() != null && !item.getCustomizationGroupIds().isEmpty()) {
            List<CustomizationGroupResponse> groups = customizationGroupRepository.findByIdIn(item.getCustomizationGroupIds())
                    .stream().map(customizationGroupMapper::toResponse).collect(Collectors.toList());
            resp.setCustomizationGroups(groups);
        }
        return resp;
    }
}
