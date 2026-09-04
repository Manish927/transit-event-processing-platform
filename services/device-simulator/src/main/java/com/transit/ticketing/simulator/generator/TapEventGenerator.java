package com.transit.ticketing.simulator.generator;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapType;
import com.transit.ticketing.simulator.dataset.TransitRecord;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import com.transit.ticketing.simulator.id.UuidV7Generator;

import java.time.Instant;

public final class TapEventGenerator {

    public EventEnvelope<TapReceivedData> generate(
            SimulatedDevice device,
            TransitRecord transitRecord,
            String credentialToken,
            TapType tapType) {

        Instant now = Instant.now();
        String eventId = UuidV7Generator.next().toString();

        EventHeader header = new EventHeader(
                eventId,
                EventType.TAP_RECEIVED,
                "1.0",
                device.state().nextSequenceNumber(),
                now,
                now,
                new Producer(
                        ProducerType.DEVICE,
                        device.deviceId(),
                        device.bootId()),
                device.agencyId(),
                eventId,
                null);

        TapReceivedData data = new TapReceivedData(
                credentialToken,
                FareMediaType.TRANSIT_CARD,
                tapType,
                transitRecord.stopId(),
                transitRecord.routeId(),
                transitRecord.tripId(),
                transitRecord.vehicleId(),
                transitRecord.stopSequence());

        return new EventEnvelope<>(header, data);
    }
}
