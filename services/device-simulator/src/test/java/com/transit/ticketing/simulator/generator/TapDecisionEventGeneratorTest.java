package com.transit.ticketing.simulator.generator;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.tap.TapAcceptedData;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapRejectedData;
import com.transit.ticketing.contracts.tap.TapRejectionReason;
import com.transit.ticketing.contracts.tap.TapType;
import com.transit.ticketing.simulator.dataset.TransitRecord;
import com.transit.ticketing.simulator.device.DeviceState;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TapDecisionEventGeneratorTest {

    @Test
    void acceptedEventShouldFollowSourceTapSequenceAndLineage() {
        SimulatedDevice device =
                new SimulatedDevice(
                        "DEVICE-001",
                        "BOOT-001",
                        "AGENCY-001",
                        new DeviceState());

        TransitRecord record =
                new TransitRecord(
                        "ROUTE-1",
                        "TRIP-1",
                        "STOP-1",
                        1,
                        "VEHICLE-1");

        EventEnvelope<TapReceivedData> sourceTap =
                new TapEventGenerator().generate(
                        device,
                        record,
                        "TOKEN-1",
                        TapType.TAP_IN);

        EventEnvelope<TapAcceptedData> accepted =
                new TapDecisionEventGenerator()
                        .generateAccepted(
                                device,
                                sourceTap);

        assertEquals(EventType.TAP_ACCEPTED,
                accepted.header().eventType());

        assertEquals(
                sourceTap.header().sequenceNumber() + 1,
                accepted.header().sequenceNumber());

        assertEquals(
                sourceTap.header().eventId(),
                accepted.header().causationId());

        assertEquals(
                sourceTap.header().correlationId(),
                accepted.header().correlationId());
    }

    @Test
    void rejectedEventShouldContainReasonCode() {
        SimulatedDevice device =
                new SimulatedDevice(
                        "DEVICE-001",
                        "BOOT-001",
                        "AGENCY-001",
                        new DeviceState());

        TransitRecord record =
                new TransitRecord(
                        "ROUTE-1",
                        "TRIP-1",
                        "STOP-1",
                        1,
                        "VEHICLE-1");

        EventEnvelope<TapReceivedData> sourceTap =
                new TapEventGenerator().generate(
                        device,
                        record,
                        "TOKEN-1",
                        TapType.TAP_IN);

        EventEnvelope<TapRejectedData> rejected =
                new TapDecisionEventGenerator()
                        .generateRejected(
                                device,
                                sourceTap,
                                TapRejectionReason.INSUFFICIENT_BALANCE);

        assertEquals(EventType.TAP_REJECTED,
                rejected.header().eventType());

        assertEquals(
                TapRejectionReason.INSUFFICIENT_BALANCE,
                rejected.data().reasonCode());
    }
}
