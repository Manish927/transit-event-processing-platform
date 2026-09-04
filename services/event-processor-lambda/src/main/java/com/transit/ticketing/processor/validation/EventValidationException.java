package com.transit.ticketing.processor.validation;

public final class EventValidationException extends RuntimeException {
    public EventValidationException(String message) {
        super(message);
    }

    public EventValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
