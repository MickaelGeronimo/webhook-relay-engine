package com.mickaelgeronimo.webhookrelay.service;

import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryEntity;
import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointEntity;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEventEntity;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEventRepository;
import com.mickaelgeronimo.webhookrelay.security.StandardWebhookSigner;
import com.mickaelgeronimo.webhookrelay.security.WebhookUrlValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages webhook endpoint registrations, secret rotation and idempotent event ingestion.
 */
@Service
public class WebhookIngestionService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookEventRepository eventRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookUrlValidator urlValidator;
    private final StandardWebhookSigner signer;
    private final Clock clock;

    public WebhookIngestionService(
            WebhookEndpointRepository endpointRepository,
            WebhookEventRepository eventRepository,
            WebhookDeliveryRepository deliveryRepository,
            WebhookUrlValidator urlValidator,
            StandardWebhookSigner signer,
            Clock clock
    ) {
        this.endpointRepository = endpointRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.urlValidator = urlValidator;
        this.signer = signer;
        this.clock = clock;
    }

    @Transactional
    public WebhookEndpointEntity registerEndpoint(
            String tenantId,
            String targetUrl,
            List<String> eventTypes,
            int maxRetries
    ) {
        String normalizedUrl = urlValidator.validate(targetUrl).toString();
        String secret = signer.generateSecret();
        WebhookEndpointEntity entity = new WebhookEndpointEntity(
                tenantId, normalizedUrl, secret, eventTypes, maxRetries);
        return endpointRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointEntity> listEndpoints(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return endpointRepository.findAll();
        }
        return endpointRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public WebhookEndpointEntity getEndpoint(String endpointId) {
        return endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint not found: " + endpointId));
    }

    @Transactional
    public WebhookEndpointEntity updateEndpointStatus(String endpointId, boolean active) {
        WebhookEndpointEntity endpoint = getEndpoint(endpointId);
        endpoint.setActive(active);
        return endpointRepository.save(endpoint);
    }

    @Transactional
    public WebhookEndpointEntity rotateEndpointSecret(String endpointId) {
        WebhookEndpointEntity endpoint = getEndpoint(endpointId);
        endpoint.rotateSecret(signer.generateSecret());
        return endpointRepository.save(endpoint);
    }

    @Transactional
    public IngestionResult ingestEvent(
            String tenantId,
            String eventType,
            String idempotencyKey,
            String payload
    ) {
        Optional<WebhookEventEntity> existing =
                eventRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            List<WebhookDeliveryEntity> existingDeliveries =
                    deliveryRepository.findAllByEventIdOrderByCreatedAtAsc(existing.get().getId());
            return new IngestionResult(existing.get(), existingDeliveries, true);
        }

        WebhookEventEntity event = eventRepository.save(
                new WebhookEventEntity(tenantId, eventType, idempotencyKey, payload));

        Instant now = clock.instant();
        List<WebhookEndpointEntity> endpoints = endpointRepository.findAllByTenantIdAndActiveTrue(tenantId);
        List<WebhookDeliveryEntity> queued = new ArrayList<>();

        for (WebhookEndpointEntity endpoint : endpoints) {
            if (endpoint.supportsEventType(eventType)) {
                WebhookDeliveryEntity delivery = new WebhookDeliveryEntity(
                        event.getId(), endpoint.getId(), endpoint.getMaxRetries(), now);
                queued.add(deliveryRepository.save(delivery));
            }
        }

        return new IngestionResult(event, queued, false);
    }

    public record IngestionResult(
            WebhookEventEntity event,
            List<WebhookDeliveryEntity> deliveries,
            boolean deduplicated
    ) {
    }
}
