package com.dequeue.menu.service;

import com.dequeue.common.security.SecurityUtils;
import com.dequeue.menu.dto.*;
import com.dequeue.menu.entity.Category;
import com.dequeue.menu.entity.MenuItem;
import com.dequeue.menu.mapper.MenuItemMapper;
import com.dequeue.menu.repository.CategoryRepository;
import com.dequeue.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the confirm step of menu image extraction:
 * resolves categories (match existing or create new) and bulk-saves menu items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuImportService {

    private final GeminiMenuExtractionService geminiService;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    /**
     * Confirms and persists an extraction session identified by {@code sessionId}.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Load extracted items from the session store.</li>
     *   <li>For each unique category name: attempt case-insensitive match against
     *       existing vendor categories; create a new one if none matches.</li>
     *   <li>Bulk-create all menu items linked to the resolved category IDs.</li>
     *   <li>Invalidate the session so it cannot be reused.</li>
     * </ol>
     *
     * @param sessionId the extraction session ID from the preview response
     * @return confirmation summary with counts and created items
     */
    public MenuExtractionConfirmResponse confirmAndSave(String sessionId) {
        String vendorId = SecurityUtils.getCurrentVendorId();

        // 1. Fetch items from the session
        List<ExtractedMenuItemDto> extractedItems = geminiService.getSessionItems(sessionId);

        // 2. Load all existing categories for this vendor once (avoid N+1)
        List<Category> existingCategories = categoryRepository.findByVendorId(vendorId);
        Map<String, Category> categoryByNormalizedName = existingCategories.stream()
                .collect(Collectors.toMap(
                        cat -> cat.getName().toLowerCase(Locale.ROOT).trim(),
                        cat -> cat,
                        (a, b) -> a // keep first on name collision
                ));

        // 3. Resolve or create categories
        int categoriesCreated = 0;
        int categoriesReused = 0;
        Map<String, String> categoryNameToId = new LinkedHashMap<>(); // categoryName → categoryId

        Set<String> distinctCategoryNames = extractedItems.stream()
                .map(ExtractedMenuItemDto::getCategoryName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String rawName : distinctCategoryNames) {
            String normalizedName = rawName.toLowerCase(Locale.ROOT).trim();

            if (categoryByNormalizedName.containsKey(normalizedName)) {
                // Reuse existing
                Category existing = categoryByNormalizedName.get(normalizedName);
                categoryNameToId.put(rawName, existing.getId());
                categoriesReused++;
                log.debug("Reusing existing category '{}' (id={})", existing.getName(), existing.getId());
            } else {
                // Create new
                Category newCategory = Category.builder()
                        .vendorId(vendorId)
                        .name(toTitleCase(rawName))
                        .description("")
                        .active(true)
                        .sortOrder(existingCategories.size() + categoriesCreated)
                        .build();
                Category saved = categoryRepository.save(newCategory);
                categoryByNormalizedName.put(normalizedName, saved); // update local map
                categoryNameToId.put(rawName, saved.getId());
                categoriesCreated++;
                log.info("Created new category '{}' for vendor {}", saved.getName(), vendorId);
            }
        }

        // 4. Bulk-create menu items
        List<MenuItem> toSave = extractedItems.stream()
                .filter(dto -> dto.getName() != null && !dto.getName().isBlank())
                .map(dto -> {
                    String catId = categoryNameToId.getOrDefault(dto.getCategoryName(),
                            categoryNameToId.values().stream().findFirst().orElse(null));

                    List<String> tags = parseTags(dto.getTags());

                    return MenuItem.builder()
                            .vendorId(vendorId)
                            .categoryId(catId)
                            .name(dto.getName().trim())
                            .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                            .price(dto.getPrice() != null ? dto.getPrice() : java.math.BigDecimal.ZERO)
                            .preparationTime(dto.getPreparationTime())
                            .tags(tags.isEmpty() ? null : tags)
                            .available(true)
                            .visible(true)
                            .sortOrder(0)
                            .build();
                })
                .collect(Collectors.toList());

        List<MenuItem> savedItems = menuItemRepository.saveAll(toSave);
        log.info("Persisted {} menu items for vendor {} from extraction session {}",
                savedItems.size(), vendorId, sessionId);

        // 5. Invalidate session (cannot be reused)
        geminiService.invalidateSession(sessionId);

        List<MenuItemResponse> responses = savedItems.stream()
                .map(menuItemMapper::toResponse)
                .collect(Collectors.toList());

        return MenuExtractionConfirmResponse.builder()
                .categoriesCreated(categoriesCreated)
                .categoriesReused(categoriesReused)
                .itemsCreated(savedItems.size())
                .createdItems(responses)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) return Collections.emptyList();
        return Arrays.stream(rawTags.split("[,;]"))
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
    }

    private String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        String[] words = input.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
