package com.transit.ticketing.ingestion.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventJson;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.device.DeviceHeartbeatReportedData;
import com.transit.ticketing.contracts.tap.TapAcceptedData;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapRejectedData;

import java.util.EnumSet;
import java.util.Set;

public final class EventValidator {

    private static final String SUPPORTED_SCHEMA_VERSION =
            "1.0";

    private static final Set<EventType>
            SUPPORTED_EVENT_TYPES =
            EnumSet.of(
                    EventType.TAP_RECEIVED,
                    EventType.TAP_ACCEPTED,
                    EventType.TAP_REJECTED,
                    EventType.DEVICE_HEARTBEAT_REPORTED);

    public EventEnvelope<?> validate(
            String rawEvent) {

        if (rawEvent == null
                || rawEvent.isBlank()) {

            throw new EventValidationException(
                    "Event payload is empty");
        }

        final EventEnvelope<JsonNode> genericEvent;

        try {
            genericEvent =
                    EventJson.fromJson(
                            rawEvent,
                            new TypeReference<
                                    EventEnvelope<JsonNode>>() {
                            });

        } catch (Exception exception) {
            throw new EventValidationException(
                    "Unable to parse event envelope",
                    exception);
        }

        if (!SUPPORTED_EVENT_TYPES.contains(
                genericEvent.header().eventType())) {

            throw new EventValidationException(
                    "Unsupported event type: "
                            + genericEvent.header()
                            .eventType());
        }

        if (!SUPPORTED_SCHEMA_VERSION.equals(
                genericEvent.header().schemaVersion())) {

            throw new EventValidationException(
                    "Unsupported schema version: "
                            + genericEvent.header()
                            .schemaVersion());
        }

        /*
         * Parse a second time into the event-specific payload.
         * This invokes the record constructors in event-contracts,
         * giving us payload-level validation as well as header
         * validation.
         */
        try {
            return switch (
                    genericEvent.header().eventType()) {

                case TAP_RECEIVED ->
                        EventJson.fromJson(
                                rawEvent,
                                new TypeReference<
                                        EventEnvelope<
                                                TapReceivedData>>() {
                                });

                case TAP_ACCEPTED ->
                        EventJson.fromJson(
                                rawEvent,
                                new TypeReference<
                                        EventEnvelope<
                                                TapAcceptedData>>() {
                                });

                case TAP_REJECTED ->
                        EventJson.fromJson(
                                rawEvent,
                                new TypeReference<
                                        EventEnvelope<
                                                TapRejectedData>>() {
                                });

                case DEVICE_HEARTBEAT_REPORTED ->
                        EventJson.fromJson(
                                rawEvent,
                                new TypeReference<
                                        EventEnvelope<
                                                DeviceHeartbeatReportedData>>() {
                                });

                default ->
                        throw new EventValidationException(
                                "Unsupported event type");
            };

        } catch (EventValidationException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new EventValidationException(
                    "Invalid event payload for "
                            + genericEvent.header()
                            .eventType(),
                    exception);
        }
    }
}
