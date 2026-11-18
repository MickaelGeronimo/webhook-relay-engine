package com.mickaelgeronimo.webhookrelay.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpointEntity, String> {

    List<WebhookEndpointEntity> findAllByTenantIdAndActiveTrue(String tenantId);

    List<WebhookEndpointEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);
}
