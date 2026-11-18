package com.mickaelgeronimo.webhookrelay.api;

/**
 * Result of triggering a manual batch dispatch run.
 */
public record DispatchSummaryResponse(int processedCount) {
}
