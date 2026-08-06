package com.dequeue.menu.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank
    private String name;
    private String description;
    private Integer sortOrder;
}
