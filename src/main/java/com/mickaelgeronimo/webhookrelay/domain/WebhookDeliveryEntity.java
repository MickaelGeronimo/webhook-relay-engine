package com.mickaelgeronimo.webhookrelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent delivery state machine linking an event to a target endpoint.
 */
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDeliveryEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "endpoint_id", nullable = false, length = 64)
    private String endpointId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebhookDeliveryEntity() {
    }

    public WebhookDeliveryEntity(String eventId, String endpointId, int maxAttempts, Instant nextAttemptAt) {
        this.id = "del_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.eventId = eventId;
        this.endpointId = endpointId;
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markDelivered(int httpStatus, Instant now) {
        this.attemptCount++;
        this.lastHttpStatus = httpStatus;
        this.lastError = null;
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = now;
    }

    public void scheduleRetry(Integer httpStatus, String errorMessage, Instant nextRetryAt) {
        this.attemptCount++;
        this.lastHttpStatus = httpStatus;
        this.lastError = truncate(errorMessage);
        this.status = DeliveryStatus.RETRYING;
        this.nextAttemptAt = nextRetryAt;
    }

    public void moveToDeadLetter(Integer httpStatus, String errorMessage) {
        this.attemptCount++;
        this.lastHttpStatus = httpStatus;
        this.lastError = truncate(errorMessage);
        this.status = DeliveryStatus.DEAD_LETTER;
    }

    public void resetForReplay(Instant now) {
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.lastError = null;
        this.lastHttpStatus = null;
        this.nextAttemptAt = now;
    }

    private String truncate(String message) {
        if (message == null || message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }

    public String getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Integer getLastHttpStatus() {
        return lastHttpStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
