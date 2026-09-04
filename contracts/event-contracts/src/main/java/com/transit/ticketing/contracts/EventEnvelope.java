package com.transit.ticketing.contracts;

import java.util.Objects;

public record EventEnvelope<T>(EventHeader header, T data) {
    public EventEnvelope {
        Objects.requireNonNull(header, "header must not be null");
        Objects.requireNonNull(data, "data must not be null");
    }
}
