package com.mickaelgeronimo.webhookrelay.service;

/**
 * Raised when a delivery operation is not valid for the delivery's current lifecycle state.
 */
public class InvalidDeliveryStateException extends RuntimeException {

    public InvalidDeliveryStateException(String message) {
        super(message);
    }
}
