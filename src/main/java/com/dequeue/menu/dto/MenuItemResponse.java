package com.dequeue.menu.dto;
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
