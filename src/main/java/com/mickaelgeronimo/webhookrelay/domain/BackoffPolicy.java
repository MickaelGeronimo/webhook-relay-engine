package com.mickaelgeronimo.webhookrelay.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Computes exponential backoff delays with bounded jitter to prevent thundering-herd retries.
 */
public class BackoffPolicy {

    private final Duration baseDelay;
    private final Duration maxDelay;
    private final double jitterRatio;
    private final DoubleSupplier randomUnitSupplier;

    public BackoffPolicy(Duration baseDelay, Duration maxDelay) {
        this(baseDelay, maxDelay, 0.25, () -> ThreadLocalRandom.current().nextDouble());
    }

    public BackoffPolicy(
            Duration baseDelay,
            Duration maxDelay,
            double jitterRatio,
            DoubleSupplier randomUnitSupplier
    ) {
        this.baseDelay = Objects.requireNonNull(baseDelay, "baseDelay must not be null");
        this.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay must not be null");
        if (baseDelay.isZero() || baseDelay.isNegative()) {
            throw new IllegalArgumentException("baseDelay must be positive");
        }
        if (maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= baseDelay");
        }
        if (jitterRatio < 0.0 || jitterRatio > 1.0) {
            throw new IllegalArgumentException("jitterRatio must be between 0.0 and 1.0");
        }
        this.jitterRatio = jitterRatio;
        this.randomUnitSupplier = Objects.requireNonNull(randomUnitSupplier, "randomUnitSupplier must not be null");
    }

    public Duration nextDelay(int failedAttemptNumber) {
        if (failedAttemptNumber <= 0) {
            throw new IllegalArgumentException("failedAttemptNumber must be positive");
        }
        int exponent = Math.min(failedAttemptNumber - 1, 30);
        long rawMillis = baseDelay.toMillis() * (1L << exponent);
        long cappedMillis = Math.min(rawMillis > 0 ? rawMillis : maxDelay.toMillis(), maxDelay.toMillis());
        double unit = Math.max(0.0, Math.min(1.0, randomUnitSupplier.getAsDouble()));
        long jitterMillis = Math.round(cappedMillis * jitterRatio * unit);
        return Duration.ofMillis(Math.min(maxDelay.toMillis(), cappedMillis + jitterMillis));
    }

    public Instant nextAttemptAt(Instant now, int failedAttemptNumber) {
        return now.plus(nextDelay(failedAttemptNumber));
    }
}
