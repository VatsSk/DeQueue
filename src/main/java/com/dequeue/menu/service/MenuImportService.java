package com.dequeue.menu.service;

import com.dequeue.common.security.SecurityUtils;
import com.dequeue.menu.dto.*;
import com.dequeue.menu.entity.Category;
import com.dequeue.menu.entity.CustomizationGroup;
import com.dequeue.menu.entity.CustomizationOption;
import com.dequeue.menu.entity.MenuItem;
import com.dequeue.menu.entity.SelectionType;
import com.dequeue.menu.mapper.MenuItemMapper;
import com.dequeue.menu.repository.CategoryRepository;
import com.dequeue.menu.repository.CustomizationGroupRepository;
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
    private final CustomizationGroupRepository customizationGroupRepository;
    private final MenuItemMapper menuItemMapper;

    /**
     * Confirms and persists an extraction session identified by {@code sessionId}.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Load extracted items from the session store.</li>
     *   <li>For each unique category name: attempt case-insensitive match against
     *       existing vendor categories; create a new one if none matches.</li>
     *   <li>For each item with detected customization groups (size/portion variants):
     *       match against existing customization groups by name, or create new ones.</li>
     *   <li>Bulk-create all menu items linked to the resolved category and customization IDs.</li>
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
                Category existing = categoryByNormalizedName.get(normalizedName);
                categoryNameToId.put(rawName, existing.getId());
                categoriesReused++;
                log.debug("Reusing existing category '{}' (id={})", existing.getName(), existing.getId());
            } else {
                Category newCategory = Category.builder()
                        .vendorId(vendorId)
                        .name(toTitleCase(rawName))
                        .description("")
                        .active(true)
                        .sortOrder(existingCategories.size() + categoriesCreated)
                        .build();
                Category saved = categoryRepository.save(newCategory);
                categoryByNormalizedName.put(normalizedName, saved);
                categoryNameToId.put(rawName, saved.getId());
                categoriesCreated++;
                log.info("Created new category '{}' for vendor {}", saved.getName(), vendorId);
            }
        }

        // 4. Resolve or create customization groups
        //    Load existing groups once; match by name (case-insensitive) to avoid duplicates.
        List<CustomizationGroup> existingGroups = customizationGroupRepository.findByVendorId(vendorId);
        Map<String, CustomizationGroup> groupByNormalizedName = new HashMap<>();
        for (CustomizationGroup g : existingGroups) {
            groupByNormalizedName.put(g.getName().toLowerCase(Locale.ROOT).trim(), g);
        }

        // item index → list of customization group IDs to attach
        Map<Integer, List<String>> itemIndexToGroupIds = new HashMap<>();

        for (int i = 0; i < extractedItems.size(); i++) {
            ExtractedMenuItemDto dto = extractedItems.get(i);
            if (dto.getCustomizationGroups() == null || dto.getCustomizationGroups().isEmpty()) {
                continue;
            }

            List<String> groupIds = new ArrayList<>();
            for (ExtractedCustomizationGroupDto groupDto : dto.getCustomizationGroups()) {
                String normalizedGroupName = groupDto.getName().toLowerCase(Locale.ROOT).trim();

                if (groupByNormalizedName.containsKey(normalizedGroupName)) {
                    // Reuse existing group
                    CustomizationGroup existing = groupByNormalizedName.get(normalizedGroupName);
                    groupIds.add(existing.getId());
                    log.debug("Reusing existing customization group '{}' (id={})", existing.getName(), existing.getId());
                } else {
                    // Build and save a new customization group
                    List<CustomizationOption> options = new ArrayList<>();
                    if (groupDto.getOptions() != null) {
                        for (ExtractedCustomizationGroupDto.Option opt : groupDto.getOptions()) {
                            options.add(CustomizationOption.builder()
                                    .name(opt.getName())
                                    .additionalPrice(opt.getAdditionalPrice() != null
                                            ? opt.getAdditionalPrice()
                                            : java.math.BigDecimal.ZERO)
                                    .available(true)
                                    .sortOrder(opt.getSortOrder())
                                    .build());
                        }
                    }

                    CustomizationGroup newGroup = CustomizationGroup.builder()
                            .vendorId(vendorId)
                            .name(toTitleCase(groupDto.getName()))
                            .selectionType(SelectionType.SINGLE)
                            .required(groupDto.isRequired())
                            .minSelection(groupDto.isRequired() ? 1 : 0)
                            .maxSelection(1)
                            .options(options)
                            .active(true)
                            .sortOrder(0)
                            .build();

                    CustomizationGroup saved = customizationGroupRepository.save(newGroup);
                    groupByNormalizedName.put(normalizedGroupName, saved); // update local cache
                    groupIds.add(saved.getId());
                    log.info("Created new customization group '{}' with {} option(s) for vendor {}",
                            saved.getName(), options.size(), vendorId);
                }
            }
            itemIndexToGroupIds.put(i, groupIds);
        }

        // 5. Bulk-create menu items
        List<MenuItem> toSave = new ArrayList<>();
        for (int i = 0; i < extractedItems.size(); i++) {
            ExtractedMenuItemDto dto = extractedItems.get(i);
            if (dto.getName() == null || dto.getName().isBlank()) continue;

            String catId = categoryNameToId.getOrDefault(dto.getCategoryName(),
                    categoryNameToId.values().stream().findFirst().orElse(null));

            List<String> tags = parseTags(dto.getTags());
            List<String> groupIds = itemIndexToGroupIds.getOrDefault(i, Collections.emptyList());

            toSave.add(MenuItem.builder()
                    .vendorId(vendorId)
                    .categoryId(catId)
                    .name(dto.getName().trim())
                    .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                    .price(dto.getPrice() != null ? dto.getPrice() : java.math.BigDecimal.ZERO)
                    .preparationTime(dto.getPreparationTime())
                    .tags(tags.isEmpty() ? null : tags)
                    .customizationGroupIds(groupIds.isEmpty() ? null : groupIds)
                    .available(true)
                    .visible(true)
                    .sortOrder(0)
                    .build());
        }

        List<MenuItem> savedItems = menuItemRepository.saveAll(toSave);
        log.info("Persisted {} menu items for vendor {} from extraction session {}",
                savedItems.size(), vendorId, sessionId);

        // 6. Invalidate session (cannot be reused)
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
