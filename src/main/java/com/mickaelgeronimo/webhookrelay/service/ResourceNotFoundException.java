package com.mickaelgeronimo.webhookrelay.service;

/**
 * Raised when a requested endpoint or delivery resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
