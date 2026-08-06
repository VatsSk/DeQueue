package com.dequeue.menu.dto;
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
