package com.mickaelgeronimo.webhookrelay.service;

/**
 * Result of executing a single outbound webhook HTTP request.
 */
public record TransportResult(
        boolean success,
        Integer httpStatus,
        String errorMessage,
        boolean retryable
) {
    public static TransportResult delivered(int httpStatus) {
        return new TransportResult(true, httpStatus, null, false);
    }

    public static TransportResult httpFailure(int httpStatus, String errorMessage, boolean retryable) {
        return new TransportResult(false, httpStatus, errorMessage, retryable);
    }

    public static TransportResult networkFailure(String errorMessage) {
        return new TransportResult(false, null, errorMessage, true);
    }
}
