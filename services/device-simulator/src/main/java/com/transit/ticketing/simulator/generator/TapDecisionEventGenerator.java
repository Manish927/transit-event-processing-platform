package com.transit.ticketing.simulator.generator;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapAcceptedData;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapRejectedData;
import com.transit.ticketing.contracts.tap.TapRejectionReason;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import com.transit.ticketing.simulator.id.UuidV7Generator;

import java.time.Instant;

public final class TapDecisionEventGenerator {

    public EventEnvelope<TapAcceptedData> generateAccepted(
            SimulatedDevice device,
            EventEnvelope<TapReceivedData> sourceTap) {

        Instant now = Instant.now();

        EventHeader header = decisionHeader(
                device,
                sourceTap,
                EventType.TAP_ACCEPTED,
                now);

        TapAcceptedData data = new TapAcceptedData(
                sourceTap.data().credentialToken(),
                sourceTap.data().mediaType(),
                sourceTap.data().tapType(),
                sourceTap.data().stopId());

        return new EventEnvelope<>(header, data);
    }

    public EventEnvelope<TapRejectedData> generateRejected(
            SimulatedDevice device,
            EventEnvelope<TapReceivedData> sourceTap,
            TapRejectionReason reason) {

        Instant now = Instant.now();

        EventHeader header = decisionHeader(
                device,
                sourceTap,
                EventType.TAP_REJECTED,
                now);

        TapRejectedData data = new TapRejectedData(
                sourceTap.data().credentialToken(),
                sourceTap.data().mediaType(),
                sourceTap.data().tapType(),
                sourceTap.data().stopId(),
                reason);

        return new EventEnvelope<>(header, data);
    }

    private EventHeader decisionHeader(
            SimulatedDevice device,
            EventEnvelope<TapReceivedData> sourceTap,
            EventType eventType,
            Instant now) {

        String eventId = UuidV7Generator.next().toString();

        return new EventHeader(
                eventId,
                eventType,
                "1.0",
                device.state().nextSequenceNumber(),
                now,
                now,
                new Producer(
                        ProducerType.DEVICE,
                        device.deviceId(),
                        device.bootId()),
                device.agencyId(),
                sourceTap.header().correlationId(),
                sourceTap.header().eventId());
    }
}
