package com.mickaelgeronimo.webhookrelay.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Payload to register a new webhook subscriber endpoint.
 */
public record RegisterEndpointRequest(
        @NotBlank String tenantId,
        @NotBlank String targetUrl,
        @NotEmpty List<@NotBlank String> eventTypes,
        @Min(1) @Max(20) Integer maxRetries
) {
    public int maxRetriesOrDefault() {
        return maxRetries == null ? 5 : maxRetries;
    }
}
