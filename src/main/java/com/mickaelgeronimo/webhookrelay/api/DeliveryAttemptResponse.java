package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.domain.DeliveryAttemptEntity;

import java.time.Instant;

/**
 * External representation of a single HTTP attempt for a delivery.
 */
public record DeliveryAttemptResponse(
        String id,
        String deliveryId,
        int attemptNumber,
        Integer httpStatus,
        long durationMs,
        String errorMessage,
        Instant attemptedAt
) {
    public static DeliveryAttemptResponse from(DeliveryAttemptEntity entity) {
        return new DeliveryAttemptResponse(
                entity.getId(),
                entity.getDeliveryId(),
                entity.getAttemptNumber(),
                entity.getHttpStatus(),
                entity.getDurationMs(),
                entity.getErrorMessage(),
                entity.getAttemptedAt()
        );
    }
}
