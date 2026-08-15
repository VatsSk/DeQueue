package com.dequeue.menu.service;

import com.dequeue.menu.dto.ExtractedCustomizationGroupDto;
import com.dequeue.menu.dto.ExtractedMenuItemDto;
import com.dequeue.menu.dto.MenuExtractionPreviewResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Calls the Gemini Flash API (free tier) with a Base64-encoded menu image,
 * parses the structured JSON response, and returns a preview of extracted items.
 *
 * <p>Sessions are stored in-memory (ConcurrentHashMap) with a 15-minute TTL.
 * For production, swap the session store with Redis using the existing Redis config.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiMenuExtractionService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";

    private static final String EXTRACTION_PROMPT = """
            You are a professional menu digitization assistant. Carefully analyze the provided menu image and extract ALL visible menu items.

            IMPORTANT – Handling size/portion variants:
            When a menu lists the SAME item multiple times with different sizes or portions (e.g. "Samosa Half ₹20 / Samosa Full ₹40", or "Tea Small ₹10 / Tea Medium ₹15 / Tea Large ₹20"), you MUST represent them as ONE item with a customizationGroups entry instead of multiple separate items.
            - Set the item's "price" to the lowest variant price (base price).
            - The additional price for each option is the option price MINUS the base price (so the cheapest option gets additionalPrice 0).
            - Common patterns to detect as customizations: Half/Full, Small/Medium/Large, Quarter/Half/Full, Regular/Large, Single/Double, etc.
            - Only create a customization group when the SAME item name (ignoring size word) appears multiple times at different prices. Do NOT create fake groups for items that have only a single price.

            Return ONLY a valid JSON object with this exact structure (no extra text, no markdown, no code fences):
            {
              "summary": "Brief description of the menu type/restaurant",
              "categories": ["Category1", "Category2"],
              "items": [
                {
                  "name": "Item Name (without the size word)",
                  "description": "Item description or empty string if not shown",
                  "price": 9.99,
                  "categoryName": "Category1",
                  "preparationTime": null,
                  "tags": "",
                  "customizationGroups": [
                    {
                      "name": "Size",
                      "required": true,
                      "options": [
                        { "name": "Half",  "additionalPrice": 0.00,  "sortOrder": 0 },
                        { "name": "Full",  "additionalPrice": 20.00, "sortOrder": 1 }
                      ]
                    }
                  ]
                }
              ]
            }

            Rules:
            - Extract every visible item, do not skip any.
            - "customizationGroups" should be an empty array [] when the item has no detected variants.
            - If price is unclear or missing, use 0.00.
            - If no categories are present, group items under "General".
            - The "tags" field should be a comma-separated string of dietary tags if visible (e.g., "veg,spicy"), otherwise empty string.
            - preparationTime is in minutes (integer), or null if not shown.
            - Prices must be numeric (no currency symbols).
            - Return ONLY the raw JSON object.
            """;


    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @jakarta.annotation.PostConstruct
    public void init() {
        // Priority 1: Check .env file directly (bypasses stale OS environment variables)
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                for (String line : java.nio.file.Files.readAllLines(envPath)) {
                    if (line.trim().startsWith("GEMINI_API_KEY=")) {
                        geminiApiKey = line.trim().substring("GEMINI_API_KEY=".length()).trim();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not read .env file for GEMINI_API_KEY", e);
        }

        // Priority 2: Check OS environment or Spring property if .env was missing/empty
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            geminiApiKey = System.getenv("GEMINI_API_KEY");
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set. Gemini API calls will fail with 403 Forbidden.");
        } else {
            // Log masked key to verify which one was picked up
            String masked = geminiApiKey.length() > 8 
                ? geminiApiKey.substring(0, 4) + "..." + geminiApiKey.substring(geminiApiKey.length() - 4)
                : "***";
            log.info("Initialized Gemini API with key: {}", masked);
        }
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * In-memory session store: sessionId → extracted items.
     * TTL logic: store creation time alongside and evict on access if expired.
     */
    private final Map<String, SessionEntry> sessionStore = new ConcurrentHashMap<>();
    private static final long SESSION_TTL_MS = 15 * 60 * 1000L; // 15 minutes

    /**
     * Sends the menu image to Gemini Flash, parses the JSON response,
     * stores the result in a temporary session, and returns the preview.
     *
     * @param imageFile the uploaded menu image
     * @return preview response with extracted items and a session ID
     */
    public MenuExtractionPreviewResponse extractMenuPreview(MultipartFile imageFile) {
        validateImageFile(imageFile);

        String base64Image;
        String mimeType;
        try {
            base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            mimeType = determineMimeType(imageFile);
        } catch (IOException e) {
            throw new MenuExtractionException("Failed to read uploaded image file", e);
        }

        String rawJson = callGeminiApi(base64Image, mimeType);
        log.debug("Raw Gemini response: {}", rawJson);

        GeminiExtractionResult result = parseGeminiResponse(rawJson);

        // Store in session
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, new SessionEntry(result.items(), System.currentTimeMillis()));

        // Evict expired sessions opportunistically
        evictExpiredSessions();

        List<String> categories = result.items().stream()
                .map(ExtractedMenuItemDto::getCategoryName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return MenuExtractionPreviewResponse.builder()
                .detectedCategories(categories)
                .items(result.items())
                .extractionSessionId(sessionId)
                .totalItems(result.items().size())
                .summary(result.summary())
                .build();
    }

    /**
     * Retrieves the session items for the given session ID.
     * Throws if session is not found or has expired.
     *
     * @param sessionId the session ID from the preview response
     * @return list of extracted items ready for persistence
     */
    public List<ExtractedMenuItemDto> getSessionItems(String sessionId) {
        SessionEntry entry = sessionStore.get(sessionId);
        if (entry == null) {
            throw new MenuExtractionException("Extraction session not found. Please re-upload the menu image.");
        }
        if (System.currentTimeMillis() - entry.createdAt() > SESSION_TTL_MS) {
            sessionStore.remove(sessionId);
            throw new MenuExtractionException("Extraction session has expired (15 min TTL). Please re-upload the menu image.");
        }
        return entry.items();
    }

    /**
     * Invalidates a session after it has been confirmed and saved.
     *
     * @param sessionId the session ID to remove
     */
    public void invalidateSession(String sessionId) {
        sessionStore.remove(sessionId);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MenuExtractionException("Image file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new MenuExtractionException("Uploaded file must be an image (JPEG, PNG, WEBP, etc.)");
        }
        // 10 MB guard (application.yml already sets multipart max but double-check)
        if (file.getSize() > 10 * 1024 * 1024L) {
            throw new MenuExtractionException("Image file exceeds 10 MB limit");
        }
    }

    private String determineMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            return contentType;
        }
        // Fallback by extension
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase();
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif"))  return "image/gif";
        return "image/jpeg"; // safe fallback
    }

    private String callGeminiApi(String base64Image, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Use standard header for API key instead of query parameter
        headers.set("x-goog-api-key", geminiApiKey);

        // Build the Gemini request body
        Map<String, Object> inlineData = new LinkedHashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Image);

        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("inlineData", inlineData);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", EXTRACTION_PROMPT);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", List.of(imagePart, textPart));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(content));

        // Generation config: ask for JSON output, limit tokens
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", 8192);
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_API_URL, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new MenuExtractionException("Gemini API returned an error: " + response.getStatusCode());
            }

            // Extract the text content from the Gemini response envelope
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode textNode = root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text");

            if (textNode.isMissingNode()) {
                log.error("Unexpected Gemini response structure: {}", response.getBody());
                throw new MenuExtractionException("Unexpected response structure from Gemini API");
            }

            return textNode.asText();

        } catch (MenuExtractionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new MenuExtractionException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private GeminiExtractionResult parseGeminiResponse(String rawJson) {
        try {
            // Strip potential markdown fences if Gemini adds them despite instructions
            String cleaned = rawJson.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
            }

            JsonNode root = objectMapper.readTree(cleaned);

            String summary = root.path("summary").asText("Menu successfully extracted");

            List<ExtractedMenuItemDto> items = new ArrayList<>();
            JsonNode itemsNode = root.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    BigDecimal price;
                    try {
                        price = new BigDecimal(itemNode.path("price").asText("0"));
                    } catch (NumberFormatException e) {
                        price = BigDecimal.ZERO;
                    }

                    Integer prepTime = null;
                    if (!itemNode.path("preparationTime").isNull() && itemNode.path("preparationTime").isInt()) {
                        prepTime = itemNode.path("preparationTime").asInt();
                    }

                    String categoryName = itemNode.path("categoryName").asText("General");
                    if (categoryName.isBlank()) categoryName = "General";

                    // ── Parse customizationGroups ──────────────────────────────
                    List<ExtractedCustomizationGroupDto> customizationGroups = new ArrayList<>();
                    JsonNode groupsNode = itemNode.path("customizationGroups");
                    if (groupsNode.isArray()) {
                        for (JsonNode groupNode : groupsNode) {
                            String groupName = groupNode.path("name").asText("Size");
                            boolean required = groupNode.path("required").asBoolean(true);

                            List<ExtractedCustomizationGroupDto.Option> options = new ArrayList<>();
                            JsonNode optionsNode = groupNode.path("options");
                            if (optionsNode.isArray()) {
                                int sortIdx = 0;
                                for (JsonNode optNode : optionsNode) {
                                    BigDecimal additionalPrice;
                                    try {
                                        additionalPrice = new BigDecimal(optNode.path("additionalPrice").asText("0"));
                                    } catch (NumberFormatException ex) {
                                        additionalPrice = BigDecimal.ZERO;
                                    }
                                    int sortOrder = optNode.path("sortOrder").asInt(sortIdx);
                                    options.add(ExtractedCustomizationGroupDto.Option.builder()
                                            .name(optNode.path("name").asText(""))
                                            .additionalPrice(additionalPrice)
                                            .sortOrder(sortOrder)
                                            .build());
                                    sortIdx++;
                                }
                            }

                            if (!options.isEmpty()) {
                                customizationGroups.add(ExtractedCustomizationGroupDto.builder()
                                        .name(groupName)
                                        .required(required)
                                        .options(options)
                                        .build());
                            }
                        }
                    }

                    items.add(ExtractedMenuItemDto.builder()
                            .name(itemNode.path("name").asText(""))
                            .description(itemNode.path("description").asText(""))
                            .price(price)
                            .categoryName(categoryName)
                            .preparationTime(prepTime)
                            .tags(itemNode.path("tags").asText(""))
                            .customizationGroups(customizationGroups.isEmpty() ? null : customizationGroups)
                            .build());
                }
            }

            if (items.isEmpty()) {
                throw new MenuExtractionException("Gemini could not detect any menu items in the image. Please try a clearer photo.");
            }

            return new GeminiExtractionResult(summary, items);

        } catch (MenuExtractionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini JSON response: {}", rawJson, e);
            throw new MenuExtractionException("Failed to parse menu extraction response. Please try again.", e);
        }
    }


    private void evictExpiredSessions() {
        long now = System.currentTimeMillis();
        sessionStore.entrySet().removeIf(entry -> now - entry.getValue().createdAt() > SESSION_TTL_MS);
    }

    // ─── Inner types ──────────────────────────────────────────────────────────

    private record SessionEntry(List<ExtractedMenuItemDto> items, long createdAt) {}

    private record GeminiExtractionResult(String summary, List<ExtractedMenuItemDto> items) {}

    /**
     * Unchecked exception for menu extraction failures with user-readable messages.
     */
    public static class MenuExtractionException extends RuntimeException {
        public MenuExtractionException(String message) { super(message); }
        public MenuExtractionException(String message, Throwable cause) { super(message, cause); }
    }
}
