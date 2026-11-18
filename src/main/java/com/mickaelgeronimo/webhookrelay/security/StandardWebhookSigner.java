package com.mickaelgeronimo.webhookrelay.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Cryptographic signer and verifier implementing the Standard Webhooks HMAC-SHA256 specification.
 *
 * <p>Signed payload format: {@code <webhook-id>.<unix-timestamp>.<body>}, encoded with
 * {@code v1,<base64-hmac-sha256>} and compared in constant time to prevent timing side-channels.
 */
public class StandardWebhookSigner {

    public static final String SECRET_PREFIX = "whsec_";
    private static final String SIGNATURE_VERSION_PREFIX = "v1,";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;
    private final Duration timestampTolerance;

    public StandardWebhookSigner() {
        this(Clock.systemUTC(), Duration.ofMinutes(5));
    }

    public StandardWebhookSigner(Clock clock, Duration timestampTolerance) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.timestampTolerance = Objects.requireNonNull(timestampTolerance, "timestampTolerance must not be null");
        if (timestampTolerance.isNegative() || timestampTolerance.isZero()) {
            throw new IllegalArgumentException("timestampTolerance must be positive");
        }
    }

    public String generateSecret() {
        byte[] raw = new byte[SECRET_BYTES];
        secureRandom.nextBytes(raw);
        return SECRET_PREFIX + Base64.getEncoder().encodeToString(raw);
    }

    public String sign(String secret, String webhookId, long epochSeconds, String payload) {
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(webhookId, "webhookId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        byte[] keyBytes = decodeSecret(secret);
        String signedContent = webhookId + "." + epochSeconds + "." + payload;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_VERSION_PREFIX + Base64.getEncoder().encodeToString(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 webhook signature", ex);
        }
    }

    public boolean verify(
            String secret,
            String webhookId,
            String timestampHeader,
            String signatureHeader,
            String payload
    ) {
        return verifyAnySecret(List.of(secret), webhookId, timestampHeader, signatureHeader, payload);
    }

    public boolean verifyAnySecret(
            List<String> secrets,
            String webhookId,
            String timestampHeader,
            String signatureHeader,
            String payload
    ) {
        if (secrets == null || secrets.isEmpty()
                || webhookId == null || webhookId.isBlank()
                || timestampHeader == null || timestampHeader.isBlank()
                || signatureHeader == null || signatureHeader.isBlank()
                || payload == null) {
            return false;
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            return false;
        }

        long nowSeconds = clock.instant().getEpochSecond();
        if (Math.abs(nowSeconds - timestampSeconds) > timestampTolerance.toSeconds()) {
            return false;
        }

        String[] candidateSignatures = signatureHeader.trim().split("\\s+");
        for (String secret : secrets) {
            if (secret == null || secret.isBlank()) {
                continue;
            }
            String expected = sign(secret, webhookId, timestampSeconds, payload);
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);

            for (String candidate : candidateSignatures) {
                if (candidate.startsWith(SIGNATURE_VERSION_PREFIX)
                        && MessageDigest.isEqual(expectedBytes, candidate.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }
        }
        return false;
    }

    private byte[] decodeSecret(String secret) {
        String encoded = secret.startsWith(SECRET_PREFIX)
                ? secret.substring(SECRET_PREFIX.length())
                : secret;
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            if (decoded.length == 0) {
                throw new IllegalArgumentException("Webhook secret must not be empty");
            }
            return decoded;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Webhook secret must be valid Base64 (optionally prefixed with whsec_)", ex);
        }
    }
}
