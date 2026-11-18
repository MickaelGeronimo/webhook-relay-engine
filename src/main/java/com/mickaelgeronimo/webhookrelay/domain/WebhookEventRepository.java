package com.mickaelgeronimo.webhookrelay.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, String> {

    Optional<WebhookEventEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}
