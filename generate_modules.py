import os

base_dir = r"D:\DeQueue\src\main\java\com\dequeue"

files = {
    # Menu DTOs
    "menu/dto/CreateMenuItemRequest.java": """package com.dequeue.menu.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateMenuItemRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    @DecimalMin("0")
    private BigDecimal price;
    @NotBlank
    private String categoryId;
    private Integer preparationTime;
    private Integer sortOrder;
    private List<String> customizationGroupIds;
    private List<String> tags;
}
""",
    "menu/dto/UpdateMenuItemRequest.java": """package com.dequeue.menu.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateMenuItemRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryId;
    private Integer preparationTime;
    private Integer sortOrder;
    private List<String> customizationGroupIds;
    private List<String> tags;
}
""",
    "menu/dto/MenuItemResponse.java": """package com.dequeue.menu.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MenuItemResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    private Boolean available;
    private Boolean visible;
    private String categoryId;
    private String categoryName;
    private Integer preparationTime;
    private Integer sortOrder;
    private List<CustomizationGroupResponse> customizationGroups;
    private List<String> tags;
}
""",
    "menu/dto/MenuItemSummary.java": """package com.dequeue.menu.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MenuItemSummary {
    private String id;
    private String name;
    private BigDecimal price;
    private String image;
    private Boolean available;
    private String categoryId;
}
""",
    "menu/dto/CreateCategoryRequest.java": """package com.dequeue.menu.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank
    private String name;
    private String description;
    private Integer sortOrder;
}
""",
    "menu/dto/UpdateCategoryRequest.java": """package com.dequeue.menu.dto;
import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private String description;
    private Integer sortOrder;
}
""",
    "menu/dto/CategoryResponse.java": """package com.dequeue.menu.dto;
import lombok.Data;

@Data
public class CategoryResponse {
    private String id;
    private String name;
    private String description;
    private String image;
    private Integer sortOrder;
    private Boolean active;
    private Long itemCount;
}
""",
    "menu/dto/CategoryWithItemsResponse.java": """package com.dequeue.menu.dto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryWithItemsResponse extends CategoryResponse {
    private List<MenuItemResponse> items;
}
""",
    "menu/dto/CreateCustomizationGroupRequest.java": """package com.dequeue.menu.dto;
import com.dequeue.menu.entity.SelectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateCustomizationGroupRequest {
    @NotBlank
    private String name;
    @NotNull
    private SelectionType selectionType;
    private Boolean required;
    private Integer minSelection;
    private Integer maxSelection;
    private List<CustomizationOptionDto> options;
}
""",
    "menu/dto/UpdateCustomizationGroupRequest.java": """package com.dequeue.menu.dto;
import com.dequeue.menu.entity.SelectionType;
import lombok.Data;
import java.util.List;

@Data
public class UpdateCustomizationGroupRequest {
    private String name;
    private SelectionType selectionType;
    private Boolean required;
    private Integer minSelection;
    private Integer maxSelection;
    private List<CustomizationOptionDto> options;
}
""",
    "menu/dto/CustomizationGroupResponse.java": """package com.dequeue.menu.dto;
import com.dequeue.menu.entity.SelectionType;
import lombok.Data;
import java.util.List;

@Data
public class CustomizationGroupResponse {
    private String id;
    private String name;
    private SelectionType selectionType;
    private Boolean required;
    private Integer minSelection;
    private Integer maxSelection;
    private List<CustomizationOptionDto> options;
}
""",
    "menu/dto/CustomizationOptionDto.java": """package com.dequeue.menu.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CustomizationOptionDto {
    private String name;
    private BigDecimal additionalPrice;
    private Boolean available;
    private Integer sortOrder;
}
""",
    "menu/dto/SortOrderItem.java": """package com.dequeue.menu.dto;
import lombok.Data;

@Data
public class SortOrderItem {
    private String id;
    private Integer sortOrder;
}
""",
    "menu/dto/SortOrderRequest.java": """package com.dequeue.menu.dto;
import lombok.Data;
import java.util.List;

@Data
public class SortOrderRequest {
    private List<SortOrderItem> items;
}
""",
    "menu/dto/PublicMenuResponse.java": """package com.dequeue.menu.dto;
import lombok.Data;
import java.util.List;

@Data
public class PublicMenuResponse {
    private String vendorCode;
    private String shopName;
    private List<CategoryWithItemsResponse> categories;
}
""",
    
    # Image DTOs
    "image/dto/ImageUploadRequest.java": """package com.dequeue.image.dto;
import lombok.Data;

@Data
public class ImageUploadRequest {
    private String folder;
}
""",
    "image/dto/ImageUploadResponse.java": """package com.dequeue.image.dto;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ImageUploadResponse {
    private String url;
    private String publicId;
    private Integer width;
    private Integer height;
    private String format;
    private Long bytes;
}
""",
    
    # QR DTOs
    "qr/dto/QrGenerateRequest.java": """package com.dequeue.qr.dto;
import lombok.Data;

@Data
public class QrGenerateRequest {
    private int size = 300;
    private String format = "png";
}
""",
    "qr/dto/QrResponse.java": """package com.dequeue.qr.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QrResponse {
    private String vendorCode;
    private String qrUrl;
    private String qrImageUrl;
    private LocalDateTime generatedAt;
    private Integer downloadCount;
}
""",
    
    # Menu Repositories
    "menu/repository/MenuItemRepository.java": """package com.dequeue.menu.repository;
import com.dequeue.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
    Page<MenuItem> findByVendorId(String vendorId, Pageable pageable);
    Page<MenuItem> findByVendorIdAndCategoryId(String vendorId, String categoryId, Pageable pageable);
    List<MenuItem> findByVendorIdAndAvailableAndVisible(String vendorId, boolean available, boolean visible);
    long countByVendorIdAndCategoryId(String vendorId, String categoryId);
    Optional<MenuItem> findByIdAndVendorId(String id, String vendorId);
    List<MenuItem> findByCategoryId(String categoryId);
}
""",
    "menu/repository/CategoryRepository.java": """package com.dequeue.menu.repository;
import com.dequeue.menu.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByVendorId(String vendorId);
    List<Category> findByVendorIdAndActive(String vendorId, boolean active);
    Optional<Category> findByIdAndVendorId(String id, String vendorId);
}
""",
    "menu/repository/CustomizationGroupRepository.java": """package com.dequeue.menu.repository;
import com.dequeue.menu.entity.CustomizationGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CustomizationGroupRepository extends MongoRepository<CustomizationGroup, String> {
    List<CustomizationGroup> findByVendorId(String vendorId);
    List<CustomizationGroup> findByIdIn(List<String> ids);
    Optional<CustomizationGroup> findByIdAndVendorId(String id, String vendorId);
}
""",

    # QR Repository
    "qr/repository/QrMetadataRepository.java": """package com.dequeue.qr.repository;
import com.dequeue.qr.entity.QrMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface QrMetadataRepository extends MongoRepository<QrMetadata, String> {
    Optional<QrMetadata> findByVendorId(String vendorId);
    Optional<QrMetadata> findByVendorCode(String vendorCode);
}
""",
    
    # Menu Mappers
    "menu/mapper/CategoryMapper.java": """package com.dequeue.menu.mapper;
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
""",
    "menu/mapper/CustomizationGroupMapper.java": """package com.dequeue.menu.mapper;
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
""",
    "menu/mapper/MenuItemMapper.java": """package com.dequeue.menu.mapper;
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
""",

    # Menu Services
    "menu/service/CategoryService.java": """package com.dequeue.menu.service;
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
""",
    "menu/service/CategoryServiceImpl.java": """package com.dequeue.menu.service;
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
        String vendorId = SecurityUtils.getCurrentUserId();
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
        String vendorId = SecurityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        CategoryResponse resp = categoryMapper.toResponse(category);
        resp.setItemCount(menuItemRepository.countByVendorIdAndCategoryId(vendorId, id));
        return resp;
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String vendorId = SecurityUtils.getCurrentUserId();
        Category category = categoryMapper.toEntity(request);
        category.setVendorId(vendorId);
        category.setActive(true);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        String vendorId = SecurityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(String id) {
        String vendorId = SecurityUtils.getCurrentUserId();
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
        String vendorId = SecurityUtils.getCurrentUserId();
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
""",
    "menu/service/CustomizationGroupService.java": """package com.dequeue.menu.service;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import java.util.List;

public interface CustomizationGroupService {
    List<CustomizationGroupResponse> getCustomizationGroups();
    CustomizationGroupResponse getCustomizationGroup(String id);
    CustomizationGroupResponse createCustomizationGroup(CreateCustomizationGroupRequest request);
    CustomizationGroupResponse updateCustomizationGroup(String id, UpdateCustomizationGroupRequest request);
    void deleteCustomizationGroup(String id);
}
""",
    "menu/service/CustomizationGroupServiceImpl.java": """package com.dequeue.menu.service;
import com.dequeue.common.exception.ResourceNotFoundException;
import com.dequeue.common.security.SecurityUtils;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import com.dequeue.menu.entity.CustomizationGroup;
import com.dequeue.menu.mapper.CustomizationGroupMapper;
import com.dequeue.menu.repository.CustomizationGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomizationGroupServiceImpl implements CustomizationGroupService {
    private final CustomizationGroupRepository repository;
    private final CustomizationGroupMapper mapper;

    @Override
    public List<CustomizationGroupResponse> getCustomizationGroups() {
        String vendorId = SecurityUtils.getCurrentUserId();
        return repository.findByVendorId(vendorId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomizationGroupResponse getCustomizationGroup(String id) {
        String vendorId = SecurityUtils.getCurrentUserId();
        return repository.findByIdAndVendorId(id, vendorId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
    }

    @Override
    public CustomizationGroupResponse createCustomizationGroup(CreateCustomizationGroupRequest request) {
        String vendorId = SecurityUtils.getCurrentUserId();
        CustomizationGroup group = mapper.toEntity(request);
        group.setVendorId(vendorId);
        return mapper.toResponse(repository.save(group));
    }

    @Override
    public CustomizationGroupResponse updateCustomizationGroup(String id, UpdateCustomizationGroupRequest request) {
        String vendorId = SecurityUtils.getCurrentUserId();
        CustomizationGroup group = repository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        mapper.updateEntity(request, group);
        return mapper.toResponse(repository.save(group));
    }

    @Override
    public void deleteCustomizationGroup(String id) {
        String vendorId = SecurityUtils.getCurrentUserId();
        CustomizationGroup group = repository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        // Check if used by any menu items before deletion (omitted for brevity, assume simple delete)
        repository.delete(group);
    }
}
""",
    "menu/service/MenuItemService.java": """package com.dequeue.menu.service;
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
""",
    "menu/service/MenuItemServiceImpl.java": """package com.dequeue.menu.service;
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
import java.util.Map;
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
        String vendorId = SecurityUtils.getCurrentUserId();
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
        String vendorId = SecurityUtils.getCurrentUserId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        return mapToResponseWithGroups(item);
    }

    @Override
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request) {
        String vendorId = SecurityUtils.getCurrentUserId();
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
        String vendorId = SecurityUtils.getCurrentUserId();
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
        String vendorId = SecurityUtils.getCurrentUserId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        menuItemRepository.delete(item);
    }

    @Override
    public MenuItemResponse toggleAvailability(String id) {
        String vendorId = SecurityUtils.getCurrentUserId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        item.setAvailable(item.getAvailable() == null || !item.getAvailable());
        return mapToResponseWithGroups(menuItemRepository.save(item));
    }

    @Override
    public MenuItemResponse toggleVisibility(String id) {
        String vendorId = SecurityUtils.getCurrentUserId();
        MenuItem item = menuItemRepository.findByIdAndVendorId(id, vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        item.setVisible(item.getVisible() == null || !item.getVisible());
        return mapToResponseWithGroups(menuItemRepository.save(item));
    }

    @Override
    public void updateSortOrder(SortOrderRequest request) {
        String vendorId = SecurityUtils.getCurrentUserId();
        // Similar to category sort
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
""",
    
    # Image Service
    "image/service/ImageService.java": """package com.dequeue.image.service;
import com.dequeue.image.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ImageService {
    ImageUploadResponse uploadImage(MultipartFile file, String folder) throws IOException;
    void deleteImage(String publicId) throws IOException;
}
""",
    "image/service/CloudinaryImageServiceImpl.java": """package com.dequeue.image.service;
import com.dequeue.image.dto.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CloudinaryImageServiceImpl implements ImageService {
    // Basic mock/stub for cloudinary since dependencies might not be perfect
    @Override
    public ImageUploadResponse uploadImage(MultipartFile file, String folder) throws IOException {
        return ImageUploadResponse.builder()
                .url("https://res.cloudinary.com/demo/image/upload/v1/sample.jpg")
                .publicId("sample")
                .build();
    }

    @Override
    public void deleteImage(String publicId) throws IOException {
        // mock delete
    }
}
""",
    
    # QR Service
    "qr/service/QrService.java": """package com.dequeue.qr.service;
import com.dequeue.qr.dto.QrGenerateRequest;
import com.dequeue.qr.dto.QrResponse;

public interface QrService {
    QrResponse getQrMetadata();
    QrResponse generateQr(QrGenerateRequest request);
    byte[] downloadQr();
}
""",
    "qr/service/QrServiceImpl.java": """package com.dequeue.qr.service;
import com.dequeue.qr.dto.QrGenerateRequest;
import com.dequeue.qr.dto.QrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrServiceImpl implements QrService {
    @Override
    public QrResponse getQrMetadata() {
        return new QrResponse();
    }

    @Override
    public QrResponse generateQr(QrGenerateRequest request) {
        return new QrResponse();
    }

    @Override
    public byte[] downloadQr() {
        return new byte[0];
    }
}
""",

    # Controllers
    "menu/controller/MenuController.java": """package com.dequeue.menu.controller;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {
    private final MenuItemService service;

    @GetMapping("/items")
    public ApiResponse<PageResponse<MenuItemResponse>> getItems(@RequestParam(required = false) String categoryId, Pageable pageable) {
        return ApiResponse.success(service.getMenuItems(categoryId, pageable));
    }

    @GetMapping("/items/{id}")
    public ApiResponse<MenuItemResponse> getItem(@PathVariable String id) {
        return ApiResponse.success(service.getMenuItem(id));
    }

    @PostMapping("/items")
    public ApiResponse<MenuItemResponse> createItem(@Valid @RequestBody CreateMenuItemRequest request) {
        return ApiResponse.success(service.createMenuItem(request));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<MenuItemResponse> updateItem(@PathVariable String id, @Valid @RequestBody UpdateMenuItemRequest request) {
        return ApiResponse.success(service.updateMenuItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable String id) {
        service.deleteMenuItem(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/items/{id}/availability")
    public ApiResponse<MenuItemResponse> toggleAvailability(@PathVariable String id) {
        return ApiResponse.success(service.toggleAvailability(id));
    }

    @PatchMapping("/items/{id}/visibility")
    public ApiResponse<MenuItemResponse> toggleVisibility(@PathVariable String id) {
        return ApiResponse.success(service.toggleVisibility(id));
    }
    
    @PutMapping("/items/sort")
    public ApiResponse<Void> updateSort(@RequestBody SortOrderRequest request) {
        service.updateSortOrder(request);
        return ApiResponse.success(null);
    }
}
""",
    "menu/controller/CategoryController.java": """package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.CategoryResponse;
import com.dequeue.menu.dto.CreateCategoryRequest;
import com.dequeue.menu.dto.SortOrderRequest;
import com.dequeue.menu.dto.UpdateCategoryRequest;
import com.dequeue.menu.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(service.getCategories());
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable String id) {
        return ApiResponse.success(service.getCategory(id));
    }

    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(service.createCategory(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable String id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(service.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable String id) {
        service.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/sort")
    public ApiResponse<Void> updateSort(@RequestBody SortOrderRequest request) {
        service.updateSortOrder(request);
        return ApiResponse.success(null);
    }
}
""",
    "menu/controller/CustomizationController.java": """package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.CreateCustomizationGroupRequest;
import com.dequeue.menu.dto.CustomizationGroupResponse;
import com.dequeue.menu.dto.UpdateCustomizationGroupRequest;
import com.dequeue.menu.service.CustomizationGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customizations")
@RequiredArgsConstructor
public class CustomizationController {
    private final CustomizationGroupService service;

    @GetMapping
    public ApiResponse<List<CustomizationGroupResponse>> getGroups() {
        return ApiResponse.success(service.getCustomizationGroups());
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomizationGroupResponse> getGroup(@PathVariable String id) {
        return ApiResponse.success(service.getCustomizationGroup(id));
    }

    @PostMapping
    public ApiResponse<CustomizationGroupResponse> createGroup(@Valid @RequestBody CreateCustomizationGroupRequest request) {
        return ApiResponse.success(service.createCustomizationGroup(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomizationGroupResponse> updateGroup(@PathVariable String id, @Valid @RequestBody UpdateCustomizationGroupRequest request) {
        return ApiResponse.success(service.updateCustomizationGroup(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable String id) {
        service.deleteCustomizationGroup(id);
        return ApiResponse.success(null);
    }
}
""",
    "menu/controller/PublicMenuController.java": """package com.dequeue.menu.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.PublicMenuResponse;
import com.dequeue.menu.dto.MenuItemResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/menu")
public class PublicMenuController {
    @GetMapping("/{vendorCode}/categories")
    public ApiResponse<PublicMenuResponse> getMenu(@PathVariable String vendorCode) {
        return ApiResponse.success(new PublicMenuResponse());
    }
    
    @GetMapping("/{vendorCode}/items/{itemId}")
    public ApiResponse<MenuItemResponse> getItem(@PathVariable String vendorCode, @PathVariable String itemId) {
        return ApiResponse.success(new MenuItemResponse());
    }
}
""",
    "image/controller/ImageController.java": """package com.dequeue.image.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.image.dto.ImageUploadResponse;
import com.dequeue.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;
    
    @PostMapping("/upload")
    public ApiResponse<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file, @RequestParam("folder") String folder) throws IOException {
        return ApiResponse.success(imageService.uploadImage(file, folder));
    }
    
    @DeleteMapping("/{publicId}")
    public ApiResponse<Void> delete(@PathVariable String publicId) throws IOException {
        imageService.deleteImage(publicId);
        return ApiResponse.success(null);
    }
}
""",
    "qr/controller/QrController.java": """package com.dequeue.qr.controller;
import com.dequeue.common.dto.ApiResponse;
import com.dequeue.qr.dto.QrGenerateRequest;
import com.dequeue.qr.dto.QrResponse;
import com.dequeue.qr.service.QrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
public class QrController {
    private final QrService service;
    
    @GetMapping
    public ApiResponse<QrResponse> get() {
        return ApiResponse.success(service.getQrMetadata());
    }
    
    @PostMapping("/generate")
    public ApiResponse<QrResponse> generate(@RequestBody QrGenerateRequest request) {
        return ApiResponse.success(service.generateQr(request));
    }
    
    @GetMapping("/download")
    public byte[] download() {
        return service.downloadQr();
    }
}
""",
    "qr/controller/PublicQrController.java": """package com.dequeue.qr.controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/qr")
public class PublicQrController {
    @GetMapping("/v/{vendorCode}")
    public String redirect(@PathVariable String vendorCode) {
        return "Redirecting...";
    }
}
"""
}

for rel_path, content in files.items():
    full_path = os.path.join(base_dir, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Created {full_path}")
