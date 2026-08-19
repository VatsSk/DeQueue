package com.dequeue.vendor.entity;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDef {
    private String name;
    private String type; // TEXT, DROPDOWN, CHECKBOX
    private boolean required;
    private List<String> options;
}
