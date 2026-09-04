package com.transit.ticketing.simulator.device;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class DeviceState {

    private final Instant bootedAt;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong totalTaps = new AtomicLong();
    private final AtomicLong acceptedTaps = new AtomicLong();
    private final AtomicLong rejectedTaps = new AtomicLong();
    private final AtomicLong gateCycles = new AtomicLong();
    private final AtomicLong readerErrors = new AtomicLong();
    private final AtomicLong motorErrors = new AtomicLong();
    private final AtomicLong communicationErrors = new AtomicLong();

    public DeviceState() {
        this(Instant.now());
    }

    DeviceState(Instant bootedAt) {
        this.bootedAt = bootedAt;
    }

    public long nextSequenceNumber() {
        return sequence.incrementAndGet();
    }

    public void recordTap(boolean accepted) {
        totalTaps.incrementAndGet();

        if (accepted) {
            acceptedTaps.incrementAndGet();
            gateCycles.incrementAndGet();
        } else {
            rejectedTaps.incrementAndGet();
        }
    }

    public long uptimeSeconds() {
        return Math.max(0L, Instant.now().getEpochSecond() - bootedAt.getEpochSecond());
    }

    public long totalTaps() {
        return totalTaps.get();
    }

    public long acceptedTaps() {
        return acceptedTaps.get();
    }

    public long rejectedTaps() {
        return rejectedTaps.get();
    }

    public long gateCycles() {
        return gateCycles.get();
    }

    public long readerErrors() {
        return readerErrors.get();
    }

    public long motorErrors() {
        return motorErrors.get();
    }

    public long communicationErrors() {
        return communicationErrors.get();
    }

    public void recordReaderError() {
        readerErrors.incrementAndGet();
    }

    public void recordMotorError() {
        motorErrors.incrementAndGet();
    }

    public void recordCommunicationError() {
        communicationErrors.incrementAndGet();
    }
}
