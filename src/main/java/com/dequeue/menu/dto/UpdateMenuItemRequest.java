package com.dequeue.menu.dto;
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
