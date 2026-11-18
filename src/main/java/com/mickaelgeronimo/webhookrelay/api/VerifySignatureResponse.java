package com.mickaelgeronimo.webhookrelay.api;

/**
 * Result of verifying a Standard Webhooks HMAC-SHA256 signature.
 */
public record VerifySignatureResponse(boolean valid) {
}
