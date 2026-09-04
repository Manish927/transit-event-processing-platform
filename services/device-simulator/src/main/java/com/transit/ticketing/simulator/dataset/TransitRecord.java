package com.transit.ticketing.simulator.dataset;

public record TransitRecord(
        String routeId,
        String tripId,
        String stopId,
        int stopSequence,
        String vehicleId) {

    public TransitRecord {
        requireText(routeId, "routeId");
        requireText(tripId, "tripId");
        requireText(stopId, "stopId");
        requireText(vehicleId, "vehicleId");

        if (stopSequence < 0) {
            throw new IllegalArgumentException("stopSequence cannot be negative");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
