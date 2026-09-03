package com.transit.ticketing.simulator.device;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceStateTest {

    @Test
    void sequenceIsSharedAndMonotonic() {
        DeviceState state = new DeviceState();

        assertEquals(1L, state.nextSequenceNumber());
        assertEquals(2L, state.nextSequenceNumber());
        assertEquals(3L, state.nextSequenceNumber());
    }

    @Test
    void countersReflectAcceptedAndRejectedTaps() {
        DeviceState state = new DeviceState();

        state.recordTap(true);
        state.recordTap(true);
        state.recordTap(false);

        assertEquals(3L, state.totalTaps());
        assertEquals(2L, state.acceptedTaps());
        assertEquals(1L, state.rejectedTaps());
        assertEquals(2L, state.gateCycles());
    }
}
