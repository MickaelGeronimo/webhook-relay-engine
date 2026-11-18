package com.mickaelgeronimo.webhookrelay.service;

import com.mickaelgeronimo.webhookrelay.domain.BackoffPolicy;
import com.mickaelgeronimo.webhookrelay.domain.DeliveryAttemptEntity;
import com.mickaelgeronimo.webhookrelay.domain.DeliveryAttemptRepository;
import com.mickaelgeronimo.webhookrelay.domain.DeliveryStatus;
import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryEntity;
import com.mickaelgeronimo.webhookrelay.domain.WebhookDeliveryRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointEntity;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEndpointRepository;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEventEntity;
import com.mickaelgeronimo.webhookrelay.domain.WebhookEventRepository;
import com.mickaelgeronimo.webhookrelay.security.StandardWebhookSigner;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Executes due webhook deliveries with HMAC-SHA256 signatures, exponential backoff retries,
 * Dead-Letter Queue (DLQ) transitions and manual DLQ replay.
 */
@Service
public class WebhookDispatcherService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEventRepository eventRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final StandardWebhookSigner signer;
    private final WebhookTransport transport;
    private final BackoffPolicy backoffPolicy;
    private final WebhookProperties properties;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public WebhookDispatcherService(
            WebhookDeliveryRepository deliveryRepository,
            WebhookEventRepository eventRepository,
            WebhookEndpointRepository endpointRepository,
            DeliveryAttemptRepository attemptRepository,
            StandardWebhookSigner signer,
            WebhookTransport transport,
            BackoffPolicy backoffPolicy,
            WebhookProperties properties,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.deliveryRepository = deliveryRepository;
        this.eventRepository = eventRepository;
        this.endpointRepository = endpointRepository;
        this.attemptRepository = attemptRepository;
        this.signer = signer;
        this.transport = transport;
        this.backoffPolicy = backoffPolicy;
        this.properties = properties;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public int dispatchDueDeliveries() {
        Instant now = clock.instant();
        List<WebhookDeliveryEntity> due = deliveryRepository
                .findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        EnumSet.of(DeliveryStatus.PENDING, DeliveryStatus.RETRYING),
                        now,
                        PageRequest.of(0, properties.getDispatcher().getBatchSize())
                );

        for (WebhookDeliveryEntity delivery : due) {
            deliverSingle(delivery.getId());
        }
        return due.size();
    }

    @Transactional
    public WebhookDeliveryEntity deliverSingle(String deliveryId) {
        WebhookDeliveryEntity delivery = getDelivery(deliveryId);
        if (delivery.getStatus() == DeliveryStatus.DELIVERED || delivery.getStatus() == DeliveryStatus.DEAD_LETTER) {
            return delivery;
        }

        WebhookEventEntity event = eventRepository.findById(delivery.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Webhook event not found: " + delivery.getEventId()));
        WebhookEndpointEntity endpoint = endpointRepository.findById(delivery.getEndpointId())
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint not found: " + delivery.getEndpointId()));

        if (!endpoint.isActive()) {
            delivery.moveToDeadLetter(null, "Target endpoint was deactivated prior to delivery");
            recordOutcome("dead_letter");
            return deliveryRepository.save(delivery);
        }

        Instant attemptStart = clock.instant();
        long epochSeconds = attemptStart.getEpochSecond();
        String signatureHeader = buildSignatureHeader(endpoint, event.getId(), epochSeconds, event.getPayload());

        long nanoStart = System.nanoTime();
        TransportResult result = transport.deliver(
                endpoint.getTargetUrl(),
                event.getId(),
                String.valueOf(epochSeconds),
                signatureHeader,
                event.getPayload()
        );
        long durationMs = Math.max(0L, (System.nanoTime() - nanoStart) / 1_000_000L);

        int attemptNumber = delivery.getAttemptCount() + 1;
        attemptRepository.save(new DeliveryAttemptEntity(
                delivery.getId(),
                attemptNumber,
                result.httpStatus(),
                durationMs,
                result.errorMessage(),
                attemptStart
        ));

        if (result.success()) {
            delivery.markDelivered(result.httpStatus(), clock.instant());
            recordOutcome("delivered");
        } else if (!result.retryable() || attemptNumber >= delivery.getMaxAttempts()) {
            delivery.moveToDeadLetter(result.httpStatus(), result.errorMessage());
            recordOutcome("dead_letter");
        } else {
            Instant nextRetryAt = backoffPolicy.nextAttemptAt(clock.instant(), attemptNumber);
            delivery.scheduleRetry(result.httpStatus(), result.errorMessage(), nextRetryAt);
            recordOutcome("retrying");
        }

        return deliveryRepository.save(delivery);
    }

    @Transactional
    public WebhookDeliveryEntity replayDeadLetter(String deliveryId) {
        WebhookDeliveryEntity delivery = getDelivery(deliveryId);
        if (delivery.getStatus() != DeliveryStatus.DEAD_LETTER) {
            throw new InvalidDeliveryStateException(
                    "Only DEAD_LETTER deliveries can be replayed; current status is " + delivery.getStatus());
        }
        delivery.resetForReplay(clock.instant());
        deliveryRepository.save(delivery);
        return deliverSingle(delivery.getId());
    }

    @Transactional(readOnly = true)
    public WebhookDeliveryEntity getDelivery(String deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook delivery not found: " + deliveryId));
    }

    @Transactional(readOnly = true)
    public List<DeliveryAttemptEntity> getAttempts(String deliveryId) {
        getDelivery(deliveryId);
        return attemptRepository.findAllByDeliveryIdOrderByAttemptNumberAsc(deliveryId);
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryEntity> listDeliveries(DeliveryStatus status) {
        if (status == null) {
            return deliveryRepository.findAll();
        }
        return deliveryRepository.findTop50ByStatusOrderByUpdatedAtDesc(status);
    }

    private String buildSignatureHeader(
            WebhookEndpointEntity endpoint,
            String webhookId,
            long epochSeconds,
            String payload
    ) {
        String primarySig = signer.sign(endpoint.getSigningSecret(), webhookId, epochSeconds, payload);
        if (endpoint.getPreviousSecret() != null && !endpoint.getPreviousSecret().isBlank()) {
            String previousSig = signer.sign(endpoint.getPreviousSecret(), webhookId, epochSeconds, payload);
            return primarySig + " " + previousSig;
        }
        return primarySig;
    }

    private void recordOutcome(String outcome) {
        meterRegistry.counter("webhook.deliveries.total", "outcome", outcome).increment();
    }
}
