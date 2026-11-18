package com.mickaelgeronimo.webhookrelay.security;

/**
 * Raised when a target webhook URL violates scheme or SSRF network boundary rules.
 */
public class UnsafeWebhookTargetException extends RuntimeException {

    public UnsafeWebhookTargetException(String message) {
        super(message);
    }
}
