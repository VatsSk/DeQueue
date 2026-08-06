package com.dequeue.order.entity;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedCustomization {
    private String groupId;
    private String groupName;
    private List<SelectedOption> selectedOptions;
}
