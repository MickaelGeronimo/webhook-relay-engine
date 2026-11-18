package com.mickaelgeronimo.webhookrelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Registered subscriber destination with HMAC signing secret(s) and event filter list.
 */
@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpointEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    @Column(name = "signing_secret", nullable = false, length = 255)
    private String signingSecret;

    @Column(name = "previous_secret", length = 255)
    private String previousSecret;

    @Column(name = "subscribed_events", nullable = false, length = 1000)
    private String subscribedEvents;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 5;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WebhookEndpointEntity() {
    }

    public WebhookEndpointEntity(
            String tenantId,
            String targetUrl,
            String signingSecret,
            List<String> eventTypes,
            int maxRetries
    ) {
        this.id = "ep_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        this.tenantId = tenantId;
        this.targetUrl = targetUrl;
        this.signingSecret = signingSecret;
        this.subscribedEvents = String.join(",", eventTypes);
        this.active = true;
        this.maxRetries = maxRetries;
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

    public boolean supportsEventType(String eventType) {
        return getSubscribedEventsList().stream()
                .anyMatch(pattern -> "*".equals(pattern) || pattern.equalsIgnoreCase(eventType));
    }

    public List<String> getSubscribedEventsList() {
        return Arrays.stream(subscribedEvents.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public String getPreviousSecret() {
        return previousSecret;
    }

    public String getSubscribedEvents() {
        return subscribedEvents;
    }

    public boolean isActive() {
        return active;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void rotateSecret(String nextSecret) {
        this.previousSecret = this.signingSecret;
        this.signingSecret = nextSecret;
    }
}
