package com.dequeue.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full preview response returned by the extract-from-image endpoint.
 * Contains all categories and items parsed from the menu image by Gemini,
 * without persisting anything yet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuExtractionPreviewResponse {

    /** Categories detected in the image (may be new or match existing ones). */
    private List<String> detectedCategories;

    /** All menu items extracted from the image. */
    private List<ExtractedMenuItemDto> items;

    /** Opaque session token used to confirm and persist the extraction. */
    private String extractionSessionId;

    /** Total items detected. */
    private int totalItems;

    /** Human-readable summary from Gemini (optional). */
    private String summary;
}
