package com.mickaelgeronimo.webhookrelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit record of a single HTTP delivery attempt for observability and debugging.
 */
@Entity
@Table(name = "delivery_attempts")
public class DeliveryAttemptEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "delivery_id", nullable = false, length = 64)
    private String deliveryId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected DeliveryAttemptEntity() {
    }

    public DeliveryAttemptEntity(
            String deliveryId,
            int attemptNumber,
            Integer httpStatus,
            long durationMs,
            String errorMessage,
            Instant attemptedAt
    ) {
        this.id = "att_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.deliveryId = deliveryId;
        this.attemptNumber = attemptNumber;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage;
        this.attemptedAt = attemptedAt;
    }

    public String getId() {
        return id;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
