package com.dequeue.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned after confirming and persisting a menu extraction session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuExtractionConfirmResponse {

    /** Number of new categories created in the database. */
    private int categoriesCreated;

    /** Number of existing categories reused (matched case-insensitively). */
    private int categoriesReused;

    /** Number of menu items persisted. */
    private int itemsCreated;

    /** The full list of created menu items. */
    private List<MenuItemResponse> createdItems;
}
