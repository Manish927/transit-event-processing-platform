package com.transit.ticketing.processor.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventJson;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.device.DeviceHeartbeatReportedData;
import com.transit.ticketing.contracts.tap.TapAcceptedData;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapRejectedData;

import java.util.Map;
import java.util.function.Function;

public final class EventValidator {

    private static final String SUPPORTED_SCHEMA_VERSION = "1.0";

    /*
     * Functional dispatch table: event type -> parser function.
     * This avoids a growing if/switch chain as new event contracts arrive.
     */
    private final Map<EventType, Function<String, EventEnvelope<?>>> parsers = Map.of(
            EventType.TAP_RECEIVED,
            json -> parse(json, new TypeReference<EventEnvelope<TapReceivedData>>() {}),

            EventType.TAP_ACCEPTED,
            json -> parse(json, new TypeReference<EventEnvelope<TapAcceptedData>>() {}),

            EventType.TAP_REJECTED,
            json -> parse(json, new TypeReference<EventEnvelope<TapRejectedData>>() {}),

            EventType.DEVICE_HEARTBEAT_REPORTED,
            json -> parse(json, new TypeReference<EventEnvelope<DeviceHeartbeatReportedData>>() {})
    );

    public EventEnvelope<?> validate(String rawEvent) {
        if (rawEvent == null || rawEvent.isBlank()) {
            throw new EventValidationException("Event payload is empty");
        }

        EventEnvelope<JsonNode> envelope = parse(
                rawEvent,
                new TypeReference<EventEnvelope<JsonNode>>() {});

        if (!SUPPORTED_SCHEMA_VERSION.equals(envelope.header().schemaVersion())) {
            throw new EventValidationException(
                    "Unsupported schema version: " + envelope.header().schemaVersion());
        }

        Function<String, EventEnvelope<?>> parser = parsers.get(envelope.header().eventType());
        if (parser == null) {
            throw new EventValidationException(
                    "Unsupported event type: " + envelope.header().eventType());
        }

        return parser.apply(rawEvent);
    }

    private static <T> T parse(String json, TypeReference<T> type) {
        try {
            return EventJson.fromJson(json, type);
        } catch (Exception exception) {
            throw new EventValidationException("Invalid event payload", exception);
        }
    }
}
