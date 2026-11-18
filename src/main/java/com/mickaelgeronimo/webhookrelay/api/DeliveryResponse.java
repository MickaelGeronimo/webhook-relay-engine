package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.domain.DeliveryStatus;
import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryEntity;

import java.time.Instant;

/**
 * External representation of a webhook delivery state.
 */
public record DeliveryResponse(
        String id,
        String eventId,
        String endpointId,
        DeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        Integer lastHttpStatus,
        String lastError,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryResponse from(WebhookDeliveryEntity entity) {
        return new DeliveryResponse(
                entity.getId(),
                entity.getEventId(),
                entity.getEndpointId(),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                entity.getNextAttemptAt(),
                entity.getLastHttpStatus(),
                entity.getLastError(),
                entity.getDeliveredAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
