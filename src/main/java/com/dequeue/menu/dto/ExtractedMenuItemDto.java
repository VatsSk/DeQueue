package com.dequeue.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents a single menu item extracted by Gemini from a menu image.
 * When Gemini detects size/portion variants (e.g. half/full, small/medium/large),
 * those are collapsed into a single item with a {@code customizationGroups} list
 * instead of creating multiple separate items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedMenuItemDto {
    private String name;
    private String description;
    /** Base price (lowest variant price, or 0 if fully driven by customization). */
    private BigDecimal price;
    private String categoryName;
    private Integer preparationTime;
    private String tags; // comma-separated raw string from Gemini

    /**
     * Customization groups detected by Gemini for this item
     * (e.g. a "Size" group with options Half / Full and their prices).
     * Null or empty when the item has no detected variants.
     */
    private List<ExtractedCustomizationGroupDto> customizationGroups;
}
