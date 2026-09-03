package com.transit.ticketing.contracts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.transit.ticketing.contracts.device.*;
import com.transit.ticketing.contracts.tap.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventContractTest {
    @Test
    void shouldSerializeAndDeserializeTapReceived() throws Exception {
        EventHeader header = new EventHeader(
                "0199a7b8-44ad-7c91-9bc3-92fd227d8741", EventType.TAP_RECEIVED, "1.0", 928771L,
                Instant.parse("2026-09-03T11:42:15.123Z"), Instant.parse("2026-09-03T11:42:15.180Z"),
                new Producer(ProducerType.DEVICE, "GATE-LON-143-02", "BOOT-991827"),
                "AGENCY-001", "0199a7b8-44ad-7c91-9bc3-92fd227d8741", null);
        TapReceivedData data = new TapReceivedData("TOKEN-884721", FareMediaType.TRANSIT_CARD, TapType.TAP_IN,
                "STOP-143", "ROUTE-27", "TRIP-8823", "VEHICLE-192", 17);
        EventEnvelope<TapReceivedData> event = new EventEnvelope<>(header, data);
        String json = EventJson.toPrettyJson(event);
        System.out.println(json);
        EventEnvelope<TapReceivedData> result = EventJson.fromJson(json, new TypeReference<EventEnvelope<TapReceivedData>>() {});
        assertEquals(EventType.TAP_RECEIVED, result.header().eventType());
        assertEquals(928771L, result.header().sequenceNumber());
        assertEquals("TOKEN-884721", result.data().credentialToken());
        assertEquals("STOP-143", result.data().stopId());
        assertEquals(17, result.data().stopSequence());
    }

    @Test
    void shouldSerializeAndDeserializeHeartbeat() throws Exception {
        EventHeader header = new EventHeader(
                "0199a7b9-bc31-7621-a989-b69bf50f76d2", EventType.DEVICE_HEARTBEAT_REPORTED, "1.0", 928772L,
                Instant.parse("2026-09-03T11:42:30Z"), Instant.parse("2026-09-03T11:42:30.025Z"),
                new Producer(ProducerType.DEVICE, "GATE-LON-143-02", "BOOT-991827"),
                "AGENCY-001", "0199a7b9-bc31-7621-a989-b69bf50f76d2", null);
        DeviceHeartbeatReportedData data = new DeviceHeartbeatReportedData(438220L,
                new DeviceCounters(8412987L, 8291832L, 121155L, 8110274L, 27L, 2L, 14L),
                new DeviceHealth(38.7, 61.2, 53.4, 47.1, 23.9), new NetworkHealth(true, 34), DeviceStatus.HEALTHY);
        EventEnvelope<DeviceHeartbeatReportedData> event = new EventEnvelope<>(header, data);
        String json = EventJson.toPrettyJson(event);
        EventEnvelope<DeviceHeartbeatReportedData> result = EventJson.fromJson(json, new TypeReference<EventEnvelope<DeviceHeartbeatReportedData>>() {});
        assertEquals(EventType.DEVICE_HEARTBEAT_REPORTED, result.header().eventType());
        assertEquals(8412987L, result.data().counters().totalTaps());
        assertEquals(DeviceStatus.HEALTHY, result.data().status());
    }
}
