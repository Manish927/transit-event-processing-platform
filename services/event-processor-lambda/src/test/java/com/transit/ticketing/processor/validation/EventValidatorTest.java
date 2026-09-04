package com.transit.ticketing.processor.validation;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventJson;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapAcceptedData;
import com.transit.ticketing.contracts.tap.TapType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventValidatorTest {

    private final EventValidator validator = new EventValidator();

    @Test
    void functionalDispatchShouldParseAcceptedTap() throws Exception {
        EventEnvelope<TapAcceptedData> event = new EventEnvelope<>(
                header(EventType.TAP_ACCEPTED, "1.0"),
                new TapAcceptedData(
                        "TOKEN-1",
                        FareMediaType.TRANSIT_CARD,
                        TapType.TAP_IN,
                        "STOP-1"));

        EventEnvelope<?> result = validator.validate(EventJson.toJson(event));
        assertEquals(EventType.TAP_ACCEPTED, result.header().eventType());
    }

    @Test
    void unsupportedSchemaMustFail() throws Exception {
        EventEnvelope<TapAcceptedData> event = new EventEnvelope<>(
                header(EventType.TAP_ACCEPTED, "2.0"),
                new TapAcceptedData(
                        "TOKEN-1",
                        FareMediaType.TRANSIT_CARD,
                        TapType.TAP_IN,
                        "STOP-1"));

        assertThrows(EventValidationException.class,
                () -> validator.validate(EventJson.toJson(event)));
    }

    private static EventHeader header(EventType type, String version) {
        return new EventHeader(
                "EVENT-1",
                type,
                version,
                1L,
                Instant.parse("2026-09-04T12:00:00Z"),
                Instant.parse("2026-09-04T12:00:00Z"),
                new Producer(ProducerType.DEVICE, "GATE-001", "BOOT-001"),
                "AGENCY-001",
                "CORR-1",
                null);
    }
}
