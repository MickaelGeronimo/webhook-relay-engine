package com.mickaelgeronimo.webhookrelay.service;

/**
 * Abstraction for outbound webhook HTTP delivery enabling deterministic integration tests.
 */
public interface WebhookTransport {

    TransportResult deliver(
            String targetUrl,
            String webhookId,
            String timestampHeader,
            String signatureHeader,
            String payload
    );
}
