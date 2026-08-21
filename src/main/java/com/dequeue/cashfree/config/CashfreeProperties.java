package com.dequeue.cashfree.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cashfree API configuration properties.
 * All secrets must be provided via environment variables — never hardcoded.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cashfree")
public class CashfreeProperties {

    /** sandbox or production */
    private String environment = "sandbox";

    /** Cashfree API Client ID — from environment variable. */
    private String clientId = "";

    /** Cashfree API Client Secret — from environment variable. NEVER log this. */
    private String clientSecret = "";

    /** Cashfree API version header value, e.g. "2023-08-01". */
    private String apiVersion = "2023-08-01";

    /** Webhook secret for signature verification. NEVER log this. */
    private String webhookSecret = "";

    /** Whether Easy Split is enabled on this Cashfree account. */
    private boolean easySplitEnabled = false;

    /** Base URL — auto-computed from environment if not overridden. */
    private String baseUrl = "";

    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return "production".equalsIgnoreCase(environment)
                ? "https://api.cashfree.com"
                : "https://sandbox.cashfree.com";
    }

    public boolean isSandbox() {
        return !"production".equalsIgnoreCase(environment);
    }
}
