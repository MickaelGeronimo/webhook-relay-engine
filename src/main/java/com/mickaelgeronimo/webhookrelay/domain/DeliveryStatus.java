package com.mickaelgeronimo.webhookrelay.domain;

/**
 * Lifecycle states of a webhook delivery task.
 */
public enum DeliveryStatus {
    PENDING,
    RETRYING,
    DELIVERED,
    DEAD_LETTER
}
