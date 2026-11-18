package com.mickaelgeronimo.webhookrelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable ingested domain event deduplicated per tenant via idempotency key.
 */
@Entity
@Table(name = "webhook_events")
public class WebhookEventEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WebhookEventEntity() {
    }

    public WebhookEventEntity(String tenantId, String eventType, String idempotencyKey, String payload) {
        this.id = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.idempotencyKey = idempotencyKey;
        this.payload = payload;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
