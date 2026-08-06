package com.dequeue.menu.dto;
import lombok.Data;

@Data
public class UpdateCategoryRequest {
    private String name;
    private String description;
    private Integer sortOrder;
}
