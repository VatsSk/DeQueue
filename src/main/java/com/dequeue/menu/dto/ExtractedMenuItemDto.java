package com.dequeue.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents a single menu item extracted by Gemini from a menu image.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedMenuItemDto {
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private Integer preparationTime;
    private String tags; // comma-separated raw string from Gemini
}
