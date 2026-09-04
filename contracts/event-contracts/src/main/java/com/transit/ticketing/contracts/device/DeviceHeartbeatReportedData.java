package com.transit.ticketing.contracts.device;

import java.util.Objects;

public record DeviceHeartbeatReportedData(long uptimeSeconds, DeviceCounters counters, DeviceHealth health, NetworkHealth network, DeviceStatus status) {
    public DeviceHeartbeatReportedData {
        if (uptimeSeconds < 0) throw new IllegalArgumentException("uptimeSeconds cannot be negative");
        Objects.requireNonNull(counters, "counters must not be null");
        Objects.requireNonNull(health, "health must not be null");
        Objects.requireNonNull(network, "network must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
