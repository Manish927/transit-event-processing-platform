package com.transit.ticketing.processor;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventJson;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapType;
import com.transit.ticketing.processor.idempotency.ClaimResult;
import com.transit.ticketing.processor.idempotency.IdempotencyStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventProcessorLambdaHandlerTest {

    @Test
    void validMessageShouldSucceed() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        IdempotencyStore store = (header, raw) -> {
            calls.incrementAndGet();
            return ClaimResult.NEW;
        };

        EventProcessorLambdaHandler handler = new EventProcessorLambdaHandler(store);
        SQSBatchResponse response = handler.handleRequest(
                sqsEvent("m-1", EventJson.toJson(validTap("EVENT-1"))),
                null);

        assertEquals(0, response.getBatchItemFailures().size());
        assertEquals(1, calls.get());
    }

    @Test
    void malformedMessageShouldBeReportedAsPartialFailure() {
        EventProcessorLambdaHandler handler = new EventProcessorLambdaHandler(
                (header, raw) -> ClaimResult.NEW);

        SQSBatchResponse response = handler.handleRequest(
                sqsEvent("bad-1", "{not-json"),
                null);

        assertEquals(1, response.getBatchItemFailures().size());
        assertEquals("bad-1",
                response.getBatchItemFailures().getFirst().getItemIdentifier());
    }

    @Test
    void oneBadRecordMustNotRetrySuccessfulRecord() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        EventProcessorLambdaHandler handler = new EventProcessorLambdaHandler(
                (header, raw) -> {
                    calls.incrementAndGet();
                    return ClaimResult.NEW;
                });

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(
                message("ok-1", EventJson.toJson(validTap("EVENT-2"))),
                message("bad-2", "bad-json")));

        SQSBatchResponse response = handler.handleRequest(event, null);

        assertEquals(1, calls.get());
        assertEquals(1, response.getBatchItemFailures().size());
        assertEquals("bad-2",
                response.getBatchItemFailures().getFirst().getItemIdentifier());
    }

    private static SQSEvent sqsEvent(String messageId, String body) {
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(message(messageId, body)));
        return event;
    }

    private static SQSEvent.SQSMessage message(String messageId, String body) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody(body);
        return message;
    }

    private static EventEnvelope<TapReceivedData> validTap(String eventId) {
        EventHeader header = new EventHeader(
                eventId,
                EventType.TAP_RECEIVED,
                "1.0",
                1L,
                Instant.parse("2026-09-04T12:00:00Z"),
                Instant.parse("2026-09-04T12:00:00Z"),
                new Producer(
                        ProducerType.DEVICE,
                        "GATE-001",
                        "BOOT-001"),
                "AGENCY-001",
                eventId,
                null);

        return new EventEnvelope<>(
                header,
                new TapReceivedData(
                        "TOKEN-1",
                        FareMediaType.TRANSIT_CARD,
                        TapType.TAP_IN,
                        "STOP-1",
                        "ROUTE-1",
                        "TRIP-1",
                        "VEHICLE-1",
                        1));
    }
}
