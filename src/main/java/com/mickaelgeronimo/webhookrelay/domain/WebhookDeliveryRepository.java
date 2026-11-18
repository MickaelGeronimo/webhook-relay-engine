package com.mickaelgeronimo.webhookrelay.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryEntity, String> {

    List<WebhookDeliveryEntity> findAllByEventIdOrderByCreatedAtAsc(String eventId);

    List<WebhookDeliveryEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            Collection<DeliveryStatus> statuses,
            Instant cutoff,
            Pageable pageable
    );

    List<WebhookDeliveryEntity> findTop50ByStatusOrderByUpdatedAtDesc(DeliveryStatus status);
}
