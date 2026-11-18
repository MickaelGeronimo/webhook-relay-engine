package com.mickaelgeronimo.webhookrelay.security;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookUrlValidatorTest {

    @Test
    void allowsPublicResolvedHostsAndRejectsInternalSsrfTargets() throws Exception {
        InetAddress publicIp = InetAddress.getByAddress("webhooks.example.com", new byte[]{(byte) 93, (byte) 184, (byte) 216, 34});
        InetAddress metadataIp = InetAddress.getByAddress("metadata.internal", new byte[]{(byte) 169, (byte) 254, (byte) 169, (byte) 254});
        InetAddress privateIp = InetAddress.getByAddress("db.internal", new byte[]{10, 0, 0, 12});

        WebhookUrlValidator strictValidator = new WebhookUrlValidator(false, host -> switch (host) {
            case "webhooks.example.com" -> List.of(publicIp);
            case "metadata.internal" -> List.of(metadataIp);
            case "db.internal" -> List.of(privateIp);
            default -> List.of(InetAddress.getLoopbackAddress());
        });

        URI valid = strictValidator.validate("https://webhooks.example.com/hooks/payments");
        assertThat(valid.getHost()).isEqualTo("webhooks.example.com");

        assertThatThrownBy(() -> strictValidator.validate("http://localhost:8080/hook"))
                .isInstanceOf(UnsafeWebhookTargetException.class);
        assertThatThrownBy(() -> strictValidator.validate("http://metadata.internal/latest/meta-data"))
                .isInstanceOf(UnsafeWebhookTargetException.class);
        assertThatThrownBy(() -> strictValidator.validate("https://db.internal/callback"))
                .isInstanceOf(UnsafeWebhookTargetException.class);
        assertThatThrownBy(() -> strictValidator.validate("ftp://webhooks.example.com/file"))
                .isInstanceOf(UnsafeWebhookTargetException.class);
        assertThatThrownBy(() -> strictValidator.validate("https://user:pass@webhooks.example.com/hook"))
                .isInstanceOf(UnsafeWebhookTargetException.class);
    }

    @Test
    void permitsLoopbackWhenExplicitlyConfiguredForLocalTesting() {
        WebhookUrlValidator permissive = new WebhookUrlValidator(true);
        URI uri = permissive.validate("http://127.0.0.1:9000/webhook");
        assertThat(uri.getPort()).isEqualTo(9000);
    }
}
