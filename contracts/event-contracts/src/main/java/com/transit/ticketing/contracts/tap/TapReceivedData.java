package com.transit.ticketing.contracts.tap;

import java.util.Objects;

public record TapReceivedData(
        String credentialToken,
        FareMediaType mediaType,
        TapType tapType,
        String stopId,
        String routeId,
        String tripId,
        String vehicleId,
        Integer stopSequence) {
    public TapReceivedData {
        requireText(credentialToken, "credentialToken");
        Objects.requireNonNull(mediaType, "mediaType must not be null");
        Objects.requireNonNull(tapType, "tapType must not be null");
        requireText(stopId, "stopId");
    }
    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
