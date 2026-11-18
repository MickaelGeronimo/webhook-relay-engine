package com.mickaelgeronimo.webhookrelay.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttemptEntity, String> {

    List<DeliveryAttemptEntity> findAllByDeliveryIdOrderByAttemptNumberAsc(String deliveryId);
}
