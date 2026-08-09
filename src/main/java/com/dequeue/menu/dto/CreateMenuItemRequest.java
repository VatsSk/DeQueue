package com.dequeue.menu.dto;
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
    private String image;
}
