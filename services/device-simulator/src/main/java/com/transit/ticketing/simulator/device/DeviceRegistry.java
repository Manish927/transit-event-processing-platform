package com.transit.ticketing.simulator.device;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeviceRegistry {

    private DeviceRegistry() {
    }

    public static List<SimulatedDevice> create(int count, String agencyId) {
        List<SimulatedDevice> devices = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {
            String deviceId = "GATE-%05d".formatted(i);
            String bootId = "BOOT-" + UUID.randomUUID();

            devices.add(new SimulatedDevice(
                    deviceId,
                    bootId,
                    agencyId,
                    new DeviceState()));
        }

        return List.copyOf(devices);
    }
}
