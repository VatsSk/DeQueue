package com.dequeue.order.service;

import com.dequeue.common.exception.BadRequestException;
import com.dequeue.vendor.entity.CustomFieldDef;
import java.util.List;
import java.util.Map;

public class CustomFieldValidator {

    public static void validate(Map<String, String> submittedValues, List<CustomFieldDef> configuredFields) {
        if (configuredFields == null || configuredFields.isEmpty()) {
            if (submittedValues != null && !submittedValues.isEmpty()) {
                throw new BadRequestException("No custom fields are configured for this vendor");
            }
            return;
        }

        if (submittedValues == null) {
            submittedValues = java.util.Collections.emptyMap();
        }

        for (CustomFieldDef field : configuredFields) {
            if (!field.isEnabled()) continue;
            
            boolean isVisible = isFieldVisible(field, configuredFields, submittedValues);
            String submittedValue = submittedValues.get(field.getId());

            if (isVisible) {
                if (field.isRequired() && (submittedValue == null || submittedValue.trim().isEmpty())) {
                    throw new BadRequestException("Field is required: " + field.getLabel());
                }
                
                if (submittedValue != null && !submittedValue.trim().isEmpty()) {
                    if (field.getType().equals("dropdown") || field.getType().equals("radio")) {
                        boolean isValidOption = false;
                        if (field.getOptions() != null) {
                            for (CustomFieldDef.CustomFieldOption opt : field.getOptions()) {
                                if (opt.getValue().equals(submittedValue)) {
                                    isValidOption = true;
                                    break;
                                }
                            }
                        }
                        if (!isValidOption) {
                            throw new BadRequestException("Invalid option for field " + field.getLabel());
                        }
                    }
                }
            } else {
                if (submittedValue != null) {
                    throw new BadRequestException("Field should not be submitted: " + field.getLabel());
                }
            }
        }
        
        // Also check if any submitted field doesn't exist
        for (String key : submittedValues.keySet()) {
            boolean found = configuredFields.stream().anyMatch(f -> f.getId().equals(key));
            if (!found) {
                throw new BadRequestException("Unknown custom field: " + key);
            }
        }
    }

    private static boolean isFieldVisible(CustomFieldDef field, List<CustomFieldDef> allFields, Map<String, String> submittedValues) {
        if (field.getConditions() == null || field.getConditions().isEmpty()) {
            return true;
        }
        
        // Currently supporting AND condition logic
        for (CustomFieldDef.CustomFieldCondition condition : field.getConditions()) {
            if (condition.getFieldId() != null && condition.getFieldId().equals(field.getId())) {
                continue; // Ignore self-referencing conditions to prevent infinite hide loops
            }
            
            String targetValue = submittedValues.get(condition.getFieldId());
            boolean conditionMet = evaluateCondition(targetValue, condition.getOperator(), condition.getValue());
            if (!conditionMet) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateCondition(String actualValue, String operator, String expectedValue) {
        if (actualValue == null) actualValue = "";
        if (expectedValue == null) expectedValue = "";
        
        if ("equals".equalsIgnoreCase(operator)) {
            return actualValue.equals(expectedValue);
        } else if ("not_equals".equalsIgnoreCase(operator)) {
            return !actualValue.equals(expectedValue);
        }
        return false;
    }
}
