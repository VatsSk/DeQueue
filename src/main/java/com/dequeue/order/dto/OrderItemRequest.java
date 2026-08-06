package com.dequeue.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class OrderItemRequest {
    @NotBlank
    private String menuItemId;
    @Min(1)
    private int quantity;
    private List<CustomizationRequest> customizations;
    private String specialInstructions;
}
