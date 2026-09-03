package com.transit.ticketing.simulator.config;

import java.util.HashMap;
import java.util.Map;

public record SimulatorProperties(
        int deviceCount,
        int passengerCount,
        int totalTaps,
        int heartbeatEveryTaps,
        long sleepMillis,
        double rejectionRate,
        String agencyId) {

    public SimulatorProperties {
        if (deviceCount <= 0) {
            throw new IllegalArgumentException("deviceCount must be > 0");
        }
        if (passengerCount <= 0) {
            throw new IllegalArgumentException("passengerCount must be > 0");
        }
        if (totalTaps < 0) {
            throw new IllegalArgumentException("totalTaps cannot be negative");
        }
        if (heartbeatEveryTaps <= 0) {
            throw new IllegalArgumentException("heartbeatEveryTaps must be > 0");
        }
        if (sleepMillis < 0) {
            throw new IllegalArgumentException("sleepMillis cannot be negative");
        }
        if (rejectionRate < 0.0 || rejectionRate > 1.0) {
            throw new IllegalArgumentException("rejectionRate must be between 0 and 1");
        }
        if (agencyId == null || agencyId.isBlank()) {
            throw new IllegalArgumentException("agencyId must not be blank");
        }
    }

    public static SimulatorProperties defaults() {
        return new SimulatorProperties(
                5,
                100,
                100,
                20,
                0,
                0.02,
                "AGENCY-001");
    }

    public static SimulatorProperties fromArgs(String[] args) {
        SimulatorProperties defaults = defaults();
        Map<String, String> values = new HashMap<>();

        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException(
                        "Unsupported argument: " + arg + ". Expected --name=value");
            }

            String[] parts = arg.substring(2).split("=", 2);
            values.put(parts[0], parts[1]);
        }

        return new SimulatorProperties(
                intValue(values, "devices", defaults.deviceCount()),
                intValue(values, "passengers", defaults.passengerCount()),
                intValue(values, "taps", defaults.totalTaps()),
                intValue(values, "heartbeat-every", defaults.heartbeatEveryTaps()),
                longValue(values, "sleep-ms", defaults.sleepMillis()),
                doubleValue(values, "rejection-rate", defaults.rejectionRate()),
                values.getOrDefault("agency", defaults.agencyId()));
    }

    private static int intValue(Map<String, String> values, String key, int defaultValue) {
        return values.containsKey(key) ? Integer.parseInt(values.get(key)) : defaultValue;
    }

    private static long longValue(Map<String, String> values, String key, long defaultValue) {
        return values.containsKey(key) ? Long.parseLong(values.get(key)) : defaultValue;
    }

    private static double doubleValue(Map<String, String> values, String key, double defaultValue) {
        return values.containsKey(key) ? Double.parseDouble(values.get(key)) : defaultValue;
    }
}
