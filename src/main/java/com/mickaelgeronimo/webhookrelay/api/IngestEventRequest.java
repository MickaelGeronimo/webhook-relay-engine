package com.mickaelgeronimo.webhookrelay.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload to publish a domain event into the webhook relay outbox.
 */
public record IngestEventRequest(
        @NotBlank String tenantId,
        @NotBlank String eventType,
        @NotBlank String idempotencyKey,
        @NotBlank String payload
) {
}
