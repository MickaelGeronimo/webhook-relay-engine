package com.mickaelgeronimo.webhookrelay.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Request asking the engine to verify a Standard Webhooks signature header.
 */
public record VerifySignatureRequest(
        @NotBlank String secret,
        @NotBlank String webhookId,
        @NotBlank String timestampHeader,
        @NotBlank String signatureHeader,
        @NotBlank String payload
) {
}
