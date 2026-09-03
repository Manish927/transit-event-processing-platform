package com.transit.ticketing.contracts.device;

public record NetworkHealth(boolean connected, int latencyMs) {
    public NetworkHealth { if (latencyMs < 0) throw new IllegalArgumentException("latencyMs cannot be negative"); }
}
