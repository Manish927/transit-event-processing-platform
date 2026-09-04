package com.transit.ticketing.contracts;

import java.time.Instant;
import java.util.Objects;

public record EventHeader(
        String eventId,
        EventType eventType,
        String schemaVersion,
        long sequenceNumber,
        Instant occurredAt,
        Instant publishedAt,
        Producer producer,
        String agencyId,
        String correlationId,
        String causationId) {

    public EventHeader {
        requireText(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType must not be null");
        requireText(schemaVersion, "schemaVersion");
        if (sequenceNumber < 0) throw new IllegalArgumentException("sequenceNumber cannot be negative");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        Objects.requireNonNull(producer, "producer must not be null");
        requireText(agencyId, "agencyId");
        requireText(correlationId, "correlationId");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
