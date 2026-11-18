package com.mickaelgeronimo.webhookrelay.api;

import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointEntity;

import java.time.Instant;
import java.util.List;

/**
 * External representation of a registered webhook endpoint.
 */
public record EndpointResponse(
        String id,
        String tenantId,
        String targetUrl,
        String signingSecret,
        List<String> subscribedEvents,
        boolean active,
        int maxRetries,
        Instant createdAt,
        Instant updatedAt
) {
    public static EndpointResponse from(WebhookEndpointEntity entity, boolean revealSecret) {
        String secret = revealSecret ? entity.getSigningSecret() : maskSecret(entity.getSigningSecret());
        return new EndpointResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getTargetUrl(),
                secret,
                entity.getSubscribedEventsList(),
                entity.isActive(),
                entity.getMaxRetries(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String maskSecret(String secret) {
        if (secret == null || secret.length() <= 12) {
            return "****";
        }
        return secret.substring(0, 10) + "..." + secret.substring(secret.length() - 4);
    }
}
