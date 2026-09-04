package com.transit.ticketing.simulator.device;

public record SimulatedDevice(
        String deviceId,
        String bootId,
        String agencyId,
        DeviceState state) {

    public SimulatedDevice {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId must not be blank");
        }
        if (bootId == null || bootId.isBlank()) {
            throw new IllegalArgumentException("bootId must not be blank");
        }
        if (agencyId == null || agencyId.isBlank()) {
            throw new IllegalArgumentException("agencyId must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }
}
