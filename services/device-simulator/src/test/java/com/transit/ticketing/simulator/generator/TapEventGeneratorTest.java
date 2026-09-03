package com.transit.ticketing.simulator.generator;

import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.tap.TapType;
import com.transit.ticketing.simulator.dataset.TransitRecord;
import com.transit.ticketing.simulator.device.DeviceState;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TapEventGeneratorTest {

    @Test
    void generatesCanonicalTapReceivedEvent() {
        SimulatedDevice device = new SimulatedDevice(
                "GATE-00001",
                "BOOT-001",
                "AGENCY-001",
                new DeviceState());

        TransitRecord transitRecord = new TransitRecord(
                "ROUTE-001",
                "TRIP-001",
                "STOP-001",
                1,
                "VEHICLE-001");

        var event = new TapEventGenerator().generate(
                device,
                transitRecord,
                "TOKEN-00000001",
                TapType.TAP_IN);

        assertEquals(EventType.TAP_RECEIVED, event.header().eventType());
        assertEquals(1L, event.header().sequenceNumber());
        assertEquals("GATE-00001", event.header().producer().producerId());
        assertEquals("STOP-001", event.data().stopId());
        assertEquals("ROUTE-001", event.data().routeId());
    }
}
