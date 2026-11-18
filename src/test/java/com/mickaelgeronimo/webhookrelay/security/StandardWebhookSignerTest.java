package com.mickaelgeronimo.webhookrelay.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardWebhookSignerTest {

    private static final Instant FIXED_NOW = Instant.ofEpochSecond(1_700_000_000L);

    @Test
    void signsAndVerifiesPayloadWithinToleranceWindow() {
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        StandardWebhookSigner signer = new StandardWebhookSigner(clock, Duration.ofMinutes(5));
        String secret = signer.generateSecret();
        assertThat(secret).startsWith(StandardWebhookSigner.SECRET_PREFIX);

        String webhookId = "msg_12345";
        String payload = "{\"event\":\"payment.settled\",\"amount\":4990}";
        long timestamp = FIXED_NOW.minusSeconds(120).getEpochSecond();

        String signature = signer.sign(secret, webhookId, timestamp, payload);
        assertThat(signature).startsWith("v1,");

        assertThat(signer.verify(secret, webhookId, String.valueOf(timestamp), signature, payload)).isTrue();
        assertThat(signer.verify(secret, webhookId, String.valueOf(timestamp), signature, payload + " ")).isFalse();
    }

    @Test
    void rejectsExpiredTimestampsAndSupportsRotatedSecrets() {
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        StandardWebhookSigner signer = new StandardWebhookSigner(clock, Duration.ofMinutes(5));
        String oldSecret = signer.generateSecret();
        String newSecret = signer.generateSecret();

        long expiredTimestamp = FIXED_NOW.minusSeconds(301).getEpochSecond();
        String expiredSig = signer.sign(newSecret, "msg_old", expiredTimestamp, "{}");
        assertThat(signer.verify(newSecret, "msg_old", String.valueOf(expiredTimestamp), expiredSig, "{}")).isFalse();

        long validTimestamp = FIXED_NOW.getEpochSecond();
        String rotatedHeader = signer.sign(oldSecret, "msg_rot", validTimestamp, "{}")
                + " " + signer.sign(newSecret, "msg_rot", validTimestamp, "{}");
        assertThat(signer.verifyAnySecret(
                List.of(newSecret), "msg_rot", String.valueOf(validTimestamp), rotatedHeader, "{}")).isTrue();
    }
}
