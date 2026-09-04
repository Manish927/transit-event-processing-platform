package com.transit.ticketing.contracts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapAcceptedData;
import com.transit.ticketing.contracts.tap.TapRejectedData;
import com.transit.ticketing.contracts.tap.TapRejectionReason;
import com.transit.ticketing.contracts.tap.TapType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TapDecisionContractTest {

    @Test
    void shouldSerializeAndDeserializeAcceptedTap() throws Exception {
        EventEnvelope<TapAcceptedData> event = new EventEnvelope<>(
                header(EventType.TAP_ACCEPTED, 1002L, "TAP-1"),
                new TapAcceptedData(
                        "TOKEN-00000001",
                        FareMediaType.TRANSIT_CARD,
                        TapType.TAP_IN,
                        "STOP-143"));

        String json = EventJson.toJson(event);

        EventEnvelope<TapAcceptedData> result = EventJson.fromJson(
                json,
                new TypeReference<EventEnvelope<TapAcceptedData>>() {});

        assertEquals(EventType.TAP_ACCEPTED, result.header().eventType());
        assertEquals("TAP-1", result.header().causationId());
        assertEquals("TOKEN-00000001", result.data().credentialToken());
    }

    @Test
    void shouldSerializeAndDeserializeRejectedTap() throws Exception {
        EventEnvelope<TapRejectedData> event = new EventEnvelope<>(
                header(EventType.TAP_REJECTED, 1002L, "TAP-1"),
                new TapRejectedData(
                        "TOKEN-00000001",
                        FareMediaType.TRANSIT_CARD,
                        TapType.TAP_IN,
                        "STOP-143",
                        TapRejectionReason.INSUFFICIENT_BALANCE));

        String json = EventJson.toJson(event);

        EventEnvelope<TapRejectedData> result = EventJson.fromJson(
                json,
                new TypeReference<EventEnvelope<TapRejectedData>>() {});

        assertEquals(EventType.TAP_REJECTED, result.header().eventType());
        assertEquals(TapRejectionReason.INSUFFICIENT_BALANCE,
                result.data().reasonCode());
    }

    private static EventHeader header(
            EventType eventType,
            long sequence,
            String causationId) {

        return new EventHeader(
                "EVENT-" + sequence,
                eventType,
                "1.0",
                sequence,
                Instant.parse("2026-09-04T04:45:00Z"),
                Instant.parse("2026-09-04T04:45:00Z"),
                new Producer(
                        ProducerType.DEVICE,
                        "GATE-001",
                        "BOOT-001"),
                "AGENCY-001",
                "CORRELATION-001",
                causationId);
    }
}
