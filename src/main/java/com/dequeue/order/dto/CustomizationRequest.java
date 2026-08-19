package com.dequeue.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CustomizationRequest {
    @NotBlank(message = "Customization groupId is required")
    private String groupId;
    @NotEmpty(message = "At least one option must be selected")
    private List<String> selectedOptionNames;
}
