package com.dequeue.menu.dto;
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
