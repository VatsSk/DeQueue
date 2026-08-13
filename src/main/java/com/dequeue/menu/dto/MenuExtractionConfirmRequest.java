package com.dequeue.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the confirm endpoint.
 * The client sends back the session ID returned by the preview endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuExtractionConfirmRequest {

    @NotBlank(message = "extractionSessionId is required")
    private String extractionSessionId;
}
