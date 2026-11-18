package com.mickaelgeronimo.webhookrelay.api;

/**
 * Payload to enable or pause deliveries to an endpoint.
 */
public record UpdateEndpointStatusRequest(boolean active) {
}
