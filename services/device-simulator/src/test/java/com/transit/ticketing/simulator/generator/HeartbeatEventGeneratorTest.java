package com.transit.ticketing.simulator.generator;

import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.simulator.device.DeviceState;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeartbeatEventGeneratorTest {

    @Test
    void heartbeatReflectsActualDeviceCountersAndSharedSequence() {
        DeviceState state = new DeviceState();
        SimulatedDevice device = new SimulatedDevice(
                "GATE-00001",
                "BOOT-001",
                "AGENCY-001",
                state);

        // Simulate a previous event from the same device.
        assertEquals(1L, state.nextSequenceNumber());

        state.recordTap(true);
        state.recordTap(true);
        state.recordTap(false);

        var heartbeat = new HeartbeatEventGenerator().generate(device);

        assertEquals(EventType.DEVICE_HEARTBEAT_REPORTED,
                heartbeat.header().eventType());
        assertEquals(2L, heartbeat.header().sequenceNumber());
        assertEquals(3L, heartbeat.data().counters().totalTaps());
        assertEquals(2L, heartbeat.data().counters().acceptedTaps());
        assertEquals(1L, heartbeat.data().counters().rejectedTaps());
    }
}
