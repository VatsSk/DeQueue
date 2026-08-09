package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.PublicMenuResponse;
import com.dequeue.menu.dto.MenuItemResponse;
import com.dequeue.menu.dto.CategoryWithItemsResponse;
import com.dequeue.menu.entity.Category;
import com.dequeue.menu.entity.MenuItem;
import com.dequeue.menu.mapper.CategoryMapper;
import com.dequeue.menu.mapper.MenuItemMapper;
import com.dequeue.menu.repository.CategoryRepository;
import com.dequeue.menu.repository.MenuItemRepository;
import com.dequeue.vendor.entity.Vendor;
import com.dequeue.vendor.repository.VendorRepository;
import com.dequeue.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/menu")
@RequiredArgsConstructor
public class PublicMenuController {
    
    private final VendorRepository vendorRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryMapper categoryMapper;
    private final MenuItemMapper menuItemMapper;
    private final com.dequeue.menu.repository.CustomizationGroupRepository customizationGroupRepository;
    private final com.dequeue.menu.mapper.CustomizationGroupMapper customizationGroupMapper;

    @GetMapping("/{vendorCode}/categories")
    public ApiResponse<PublicMenuResponse> getMenu(@PathVariable String vendorCode) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        List<Category> categories = categoryRepository.findByVendorId(vendor.getId());
        // Fetch all items for the vendor, we'll let the frontend or business logic decide if they are visible
        // But to be safe and show items if the DB is missing boolean flags, we'll just send them.
        List<MenuItem> items = menuItemRepository.findByVendorId(vendor.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();

        List<CategoryWithItemsResponse> categoryResponses = categories.stream().map(cat -> {
            CategoryWithItemsResponse resp = categoryMapper.toWithItemsResponse(cat);
            List<MenuItemResponse> catItems = items.stream()
                    .filter(item -> cat.getId().equals(item.getCategoryId()))
                    .map(item -> {
                        MenuItemResponse itemResp = menuItemMapper.toResponse(item);
                        if (item.getCustomizationGroupIds() != null && !item.getCustomizationGroupIds().isEmpty()) {
                            List<com.dequeue.menu.dto.CustomizationGroupResponse> groups = customizationGroupRepository.findByIdIn(item.getCustomizationGroupIds())
                                    .stream().map(customizationGroupMapper::toResponse).collect(Collectors.toList());
                            itemResp.setCustomizationGroups(groups);
                        }
                        return itemResp;
                    })
                    .collect(Collectors.toList());
            resp.setItems(catItems);
            return resp;
        }).collect(Collectors.toList());

        PublicMenuResponse response = new PublicMenuResponse();
        response.setVendorCode(vendor.getVendorCode());
        response.setShopName(vendor.getShopName());
        response.setCategories(categoryResponses);

        return ApiResponse.success(response);
    }
    
    @GetMapping("/{vendorCode}/items/{itemId}")
    public ApiResponse<MenuItemResponse> getItem(@PathVariable String vendorCode, @PathVariable String itemId) {
        Vendor vendor = vendorRepository.findByVendorCode(vendorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        MenuItem item = menuItemRepository.findByIdAndVendorId(itemId, vendor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        
        MenuItemResponse itemResp = menuItemMapper.toResponse(item);
        if (item.getCustomizationGroupIds() != null && !item.getCustomizationGroupIds().isEmpty()) {
            List<com.dequeue.menu.dto.CustomizationGroupResponse> groups = customizationGroupRepository.findByIdIn(item.getCustomizationGroupIds())
                    .stream().map(customizationGroupMapper::toResponse).collect(Collectors.toList());
            itemResp.setCustomizationGroups(groups);
        }
        
        return ApiResponse.success(itemResp);
    }
}
