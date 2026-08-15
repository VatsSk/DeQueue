package com.dequeue.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * A customization group extracted by Gemini for a single menu item.
 * Typically represents size/portion variants such as Half/Full or S/M/L,
 * but can also represent add-on groups (e.g. "Toppings").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedCustomizationGroupDto {

    /** Human-readable group label, e.g. "Size", "Portion", "Toppings". */
    private String name;

    /**
     * Whether exactly one option must be selected.
     * True for size/portion groups; false for optional add-on groups.
     */
    @Builder.Default
    private boolean required = true;

    /** Options within this group with their price differences relative to the base price. */
    private List<Option> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        /** Option label, e.g. "Half", "Full", "Small", "Medium", "Large". */
        private String name;

        /**
         * Price delta relative to the item base price.
         * For the cheapest option this is typically 0.00;
         * for larger variants it's the extra amount charged.
         */
        @Builder.Default
        private BigDecimal additionalPrice = BigDecimal.ZERO;

        /** Sort position (0-based, cheapest/smallest first). */
        @Builder.Default
        private int sortOrder = 0;
    }
}
