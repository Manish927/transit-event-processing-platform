package com.transit.ticketing.ingestion.validation;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventJson;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapRejectedData;
import com.transit.ticketing.contracts.tap.TapRejectionReason;
import com.transit.ticketing.contracts.tap.TapType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventValidatorTest {

    private final EventValidator validator =
            new EventValidator();

    @Test
    void shouldValidateRejectedTapPayload()
            throws Exception {

        EventEnvelope<TapRejectedData> event =
                new EventEnvelope<>(
                        header(
                                EventType.TAP_REJECTED,
                                "1.0"),
                        new TapRejectedData(
                                "TOKEN-1",
                                FareMediaType.TRANSIT_CARD,
                                TapType.TAP_IN,
                                "STOP-1",
                                TapRejectionReason
                                        .INSUFFICIENT_BALANCE));

        EventEnvelope<?> result =
                validator.validate(
                        EventJson.toJson(event));

        assertEquals(
                EventType.TAP_REJECTED,
                result.header().eventType());
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion()
            throws Exception {

        EventEnvelope<TapRejectedData> event =
                new EventEnvelope<>(
                        header(
                                EventType.TAP_REJECTED,
                                "2.0"),
                        new TapRejectedData(
                                "TOKEN-1",
                                FareMediaType.TRANSIT_CARD,
                                TapType.TAP_IN,
                                "STOP-1",
                                TapRejectionReason
                                        .MEDIA_BLOCKED));

        assertThrows(
                EventValidationException.class,
                () -> validator.validate(
                        EventJson.toJson(event)));
    }

    private static EventHeader header(
            EventType eventType,
            String schemaVersion) {

        return new EventHeader(
                "EVENT-1",
                eventType,
                schemaVersion,
                1L,
                Instant.parse(
                        "2026-09-04T05:00:00Z"),
                Instant.parse(
                        "2026-09-04T05:00:00Z"),
                new Producer(
                        ProducerType.DEVICE,
                        "DEVICE-001",
                        "BOOT-001"),
                "AGENCY-001",
                "CORR-001",
                "SOURCE-EVENT-1");
    }
}
