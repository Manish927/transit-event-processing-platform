package com.transit.ticketing.contracts.tap;

import java.util.Objects;

public record TapRejectedData(
        String credentialToken,
        FareMediaType mediaType,
        TapType tapType,
        String stopId,
        TapRejectionReason reasonCode) {

    public TapRejectedData {
        requireText(credentialToken, "credentialToken");
        Objects.requireNonNull(mediaType, "mediaType must not be null");
        Objects.requireNonNull(tapType, "tapType must not be null");
        requireText(stopId, "stopId");
        Objects.requireNonNull(reasonCode, "reasonCode must not be null");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
