package com.dequeue.menu.service;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.exception.BadRequestException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.menu.dto.CategoryResponse;
import com.dequeue.menu.dto.CreateCategoryRequest;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateCategoryRequest;
import com.dequeue.menu.entity.Category;
import com.dequeue.menu.mapper.CategoryMapper;
import com.dequeue.menu.repository.CategoryRepository;
import com.dequeue.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryMapper categoryMapper;
    
    @Override
    public List<CategoryResponse> getCategories() {
        String vendorId = SecurityUtils.getCurrentVendorId();
        return categoryRepository.findByVendorId(vendorId).stream()
                .map(cat -> {
                    CategoryResponse resp = categoryMapper.toResponse(cat);
                    resp.setItemCount(menuItemRepository.countByVendorIdAndCategoryId(vendorId, cat.getId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategory(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Category category = categoryRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        CategoryResponse resp = categoryMapper.toResponse(category);
        resp.setItemCount(menuItemRepository.countByVendorIdAndCategoryId(vendorId, id));
        return resp;
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Category category = categoryMapper.toEntity(request);
        category.setVendorId(vendorId);
        category.setActive(true);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Category category = categoryRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(String id) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        Category category = categoryRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        long count = menuItemRepository.countByVendorIdAndCategoryId(vendorId, id);
        if (count > 0) {
            throw new BadRequestException("Cannot delete category with active items");
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public void updateSortOrder(SortOrderRequest request) {
        String vendorId = SecurityUtils.getCurrentVendorId();
        List<Category> categories = categoryRepository.findByVendorId(vendorId);
        Map<String, Category> categoryMap = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
        request.getItems().forEach(item -> {
            if (categoryMap.containsKey(item.getId())) {
                Category cat = categoryMap.get(item.getId());
                cat.setSortOrder(item.getSortOrder());
                categoryRepository.save(cat);
            }
        });
    }
}
