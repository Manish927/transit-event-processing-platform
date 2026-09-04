package com.transit.ticketing.contracts.device;

public record DeviceHealth(double cpuUtilizationPct, double memoryUtilizationPct, double cpuTemperatureC, double motorTemperatureC, double supplyVoltage) {
    public DeviceHealth {
        percent(cpuUtilizationPct, "cpuUtilizationPct");
        percent(memoryUtilizationPct, "memoryUtilizationPct");
    }
    private static void percent(double value, String field) { if (value < 0 || value > 100) throw new IllegalArgumentException(field + " must be between 0 and 100"); }
}
