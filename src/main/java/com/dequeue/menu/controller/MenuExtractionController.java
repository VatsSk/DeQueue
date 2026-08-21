package com.dequeue.menu.controller;

import com.dequeue.common.dto.ApiResponse;
import com.dequeue.menu.dto.MenuExtractionConfirmRequest;
import com.dequeue.menu.dto.MenuExtractionConfirmResponse;
import com.dequeue.menu.dto.MenuExtractionPreviewResponse;
import com.dequeue.menu.service.GeminiMenuExtractionService;
import com.dequeue.menu.service.MenuImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Exposes the AI-powered menu extraction endpoints.
 *
 * <p>Workflow:
 * <ol>
 *   <li>{@code POST /api/v1/menu/extract-from-image} — Upload menu photo → returns preview + sessionId</li>
 *   <li>{@code POST /api/v1/menu/extract-from-image/confirm} — Send sessionId → saves to DB</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Tag(name = "Menu Extraction", description = "AI-powered menu extraction from images using Gemini Flash")
public class MenuExtractionController {

    private final GeminiMenuExtractionService geminiService;
    private final MenuImportService menuImportService;

    /**
     * Step 1 — Upload a menu image to get an extraction preview.
     *
     * <p>The image is sent to Gemini Flash which reads the menu and returns a
     * structured list of categories and items. Nothing is persisted at this stage.
     * The response contains an {@code extractionSessionId} that must be sent to
     * the confirm endpoint within 15 minutes.
     *
     * @param image the menu photo (JPEG / PNG / WEBP, max 10 MB)
     * @return preview with detected categories, items, and a session ID
     */
    @Operation(
        summary = "Extract menu from image (preview)",
        description = "Upload a menu photo. Gemini Flash will extract all visible categories and items. " +
                      "Returns a preview and a session ID to confirm saving. Session expires in 15 minutes."
    )
    @PostMapping(value = "/extract-from-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MenuExtractionPreviewResponse> extractMenuFromImage(
            @Parameter(
                description = "Menu photo (JPEG, PNG, or WEBP, max 10 MB)",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                                   schema = @Schema(type = "string", format = "binary"))
            )
            @RequestPart("image") MultipartFile image) {

        log.info("Menu extraction request received: file='{}', size={} bytes",
                image.getOriginalFilename(), image.getSize());

        MenuExtractionPreviewResponse preview = geminiService.extractMenuPreview(image);

        log.info("Extraction preview ready: {} items detected, sessionId={}",
                preview.getTotalItems(), preview.getExtractionSessionId());

        return ApiResponse.success(preview,
                String.format("Successfully extracted %d items from your menu image. " +
                              "Review the preview and confirm to save.", preview.getTotalItems()));
    }

    /**
     * Step 2 — Confirm extraction and persist items to the database.
     *
     * <p>Sends the {@code extractionSessionId} from the preview step. The service
     * will resolve categories (reuse existing ones by name or create new ones) and
     * bulk-create all menu items. The session is invalidated after this call.
     *
     * @param request contains the extraction session ID
     * @return summary of categories created/reused and items persisted
     */
    @Operation(
        summary = "Confirm menu extraction and save to database",
        description = "Send the extractionSessionId obtained from the preview endpoint to persist all " +
                      "extracted categories and menu items. Existing categories are matched by name " +
                      "(case-insensitive) and reused. New categories are created automatically."
    )
    @PostMapping("/extract-from-image/confirm")
    public ApiResponse<MenuExtractionConfirmResponse> confirmMenuExtraction(
            @Valid @RequestBody MenuExtractionConfirmRequest request) {

        log.info("Confirming menu extraction for sessionId={}", request.getExtractionSessionId());
        MenuExtractionConfirmResponse result = menuImportService.confirmAndSave(request);

        log.info("Menu extraction confirmed: {} items created, {} categories created, {} reused",
                result.getItemsCreated(), result.getCategoriesCreated(), result.getCategoriesReused());

        return ApiResponse.success(result,
                String.format("Menu saved successfully! Created %d items across %d new and %d existing categories.",
                        result.getItemsCreated(), result.getCategoriesCreated(), result.getCategoriesReused()));
    }
}
