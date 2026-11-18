package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryEntity;
import com.mickaelgeronimo.webhookrelay.service.WebhookIngestionService;

import java.util.List;

/**
 * Summary of an ingested event and all queued subscriber deliveries.
 */
public record IngestEventResponse(
        String eventId,
        String tenantId,
        String eventType,
        String idempotencyKey,
        boolean deduplicated,
        List<String> deliveryIds
) {
    public static IngestEventResponse from(WebhookIngestionService.IngestionResult result) {
        return new IngestEventResponse(
                result.event().getId(),
                result.event().getTenantId(),
                result.event().getEventType(),
                result.event().getIdempotencyKey(),
                result.deduplicated(),
                result.deliveries().stream().map(WebhookDeliveryEntity::getId).toList()
        );
    }
}
