package com.transit.ticketing.contracts;

import java.util.Objects;

public record Producer(ProducerType producerType, String producerId, String producerInstanceId) {
    public Producer {
        Objects.requireNonNull(producerType, "producerType must not be null");
        requireText(producerId, "producerId");
        requireText(producerInstanceId, "producerInstanceId");
    }
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
