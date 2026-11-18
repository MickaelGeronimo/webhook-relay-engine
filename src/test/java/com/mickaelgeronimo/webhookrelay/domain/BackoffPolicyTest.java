package com.mickaelgeronimo.webhookrelay.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffPolicyTest {

    @Test
    void doublesDelayWithExponentialBackoffAndCapsAtMaximum() {
        BackoffPolicy noJitter = new BackoffPolicy(Duration.ofSeconds(5), Duration.ofSeconds(60), 0.0, () -> 0.0);

        assertThat(noJitter.nextDelay(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(noJitter.nextDelay(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(noJitter.nextDelay(3)).isEqualTo(Duration.ofSeconds(20));
        assertThat(noJitter.nextDelay(4)).isEqualTo(Duration.ofSeconds(40));
        assertThat(noJitter.nextDelay(5)).isEqualTo(Duration.ofSeconds(60));
        assertThat(noJitter.nextDelay(10)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void appliesBoundedJitterWithinMaxDelay() {
        BackoffPolicy withJitter = new BackoffPolicy(Duration.ofSeconds(10), Duration.ofSeconds(30), 0.25, () -> 1.0);

        assertThat(withJitter.nextDelay(1)).isEqualTo(Duration.ofMillis(12_500));
        assertThat(withJitter.nextDelay(2)).isEqualTo(Duration.ofMillis(25_000));
        assertThat(withJitter.nextDelay(3)).isEqualTo(Duration.ofSeconds(30));
    }
}
