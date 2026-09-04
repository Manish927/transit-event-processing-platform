package com.transit.ticketing.contracts.device;

public record DeviceCounters(long totalTaps, long acceptedTaps, long rejectedTaps, long gateCycles, long readerErrors, long motorErrors, long communicationErrors) {
    public DeviceCounters {
        nonNegative(totalTaps, "totalTaps"); nonNegative(acceptedTaps, "acceptedTaps"); nonNegative(rejectedTaps, "rejectedTaps");
        nonNegative(gateCycles, "gateCycles"); nonNegative(readerErrors, "readerErrors"); nonNegative(motorErrors, "motorErrors"); nonNegative(communicationErrors, "communicationErrors");
    }
    private static void nonNegative(long value, String field) { if (value < 0) throw new IllegalArgumentException(field + " cannot be negative"); }
}
