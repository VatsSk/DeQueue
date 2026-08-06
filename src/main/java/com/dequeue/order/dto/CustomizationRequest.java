package com.dequeue.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class CustomizationRequest {
    private String groupId;
    private List<String> selectedOptionNames;
}
