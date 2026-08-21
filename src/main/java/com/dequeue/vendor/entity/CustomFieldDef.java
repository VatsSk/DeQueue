package com.dequeue.vendor.entity;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDef {
    private String id;
    private String label;
    private String type;
    private boolean required;
    private boolean enabled;
    private int displayOrder;
    private List<CustomFieldOption> options;
    private String placeholder;
    private String helpText;
    private List<CustomFieldCondition> conditions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomFieldOption {
        private String value;
        private String label;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomFieldCondition {
        private String fieldId;
        private String operator; // equals, not_equals, etc.
        private String value;
    }
}
