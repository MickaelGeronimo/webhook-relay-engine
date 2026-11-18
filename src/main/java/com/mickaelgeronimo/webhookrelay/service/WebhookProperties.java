package com.mickaelgeronimo.webhookrelay.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration properties for webhook security and retry dispatching.
 */
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    private Security security = new Security();
    private Dispatcher dispatcher = new Dispatcher();

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public static class Security {
        private boolean allowPrivateTargets = false;
        private long signatureToleranceSeconds = 300;

        public boolean isAllowPrivateTargets() {
            return allowPrivateTargets;
        }

        public void setAllowPrivateTargets(boolean allowPrivateTargets) {
            this.allowPrivateTargets = allowPrivateTargets;
        }

        public long getSignatureToleranceSeconds() {
            return signatureToleranceSeconds;
        }

        public void setSignatureToleranceSeconds(long signatureToleranceSeconds) {
            this.signatureToleranceSeconds = signatureToleranceSeconds;
        }
    }

    public static class Dispatcher {
        private boolean schedulingEnabled = true;
        private long fixedDelayMs = 5000;
        private int batchSize = 50;
        private long baseRetryDelaySeconds = 5;
        private long maxRetryDelaySeconds = 300;

        public boolean isSchedulingEnabled() {
            return schedulingEnabled;
        }

        public void setSchedulingEnabled(boolean schedulingEnabled) {
            this.schedulingEnabled = schedulingEnabled;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getBaseRetryDelaySeconds() {
            return baseRetryDelaySeconds;
        }

        public void setBaseRetryDelaySeconds(long baseRetryDelaySeconds) {
            this.baseRetryDelaySeconds = baseRetryDelaySeconds;
        }

        public long getMaxRetryDelaySeconds() {
            return maxRetryDelaySeconds;
        }

        public void setMaxRetryDelaySeconds(long maxRetryDelaySeconds) {
            this.maxRetryDelaySeconds = maxRetryDelaySeconds;
        }
    }
}
