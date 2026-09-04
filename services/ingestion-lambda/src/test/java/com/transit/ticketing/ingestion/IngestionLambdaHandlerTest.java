package com.transit.ticketing.ingestion;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventJson;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.tap.FareMediaType;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapType;
import com.transit.ticketing.ingestion.publisher.EventPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionLambdaHandlerTest {

    @Test
    void shouldAcceptValidTapAndPreserveRawJson()
            throws Exception {

        AtomicReference<String> publishedBody =
                new AtomicReference<>();

        EventPublisher publisher =
                (rawEvent, header) -> {
                    publishedBody.set(rawEvent);
                    return "SQS-MESSAGE-001";
                };

        IngestionLambdaHandler handler =
                new IngestionLambdaHandler(publisher);

        String json =
                EventJson.toJson(validTapReceived());

        APIGatewayV2HTTPEvent request =
                httpRequest(json);

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        request,
                        null);

        assertEquals(
                202,
                response.getStatusCode());

        assertEquals(
                json,
                publishedBody.get());

        assertTrue(
                response.getBody()
                        .contains("\"status\":\"ACCEPTED\""));
    }

    @Test
    void shouldRejectUnsupportedEventType()
            throws Exception {

        EventPublisher publisher =
                (rawEvent, header) -> {
                    throw new AssertionError(
                            "Publisher must not be called");
                };

        IngestionLambdaHandler handler =
                new IngestionLambdaHandler(publisher);

        EventEnvelope<TapReceivedData> event =
                new EventEnvelope<>(
                        new EventHeader(
                                "EVENT-2",
                                EventType.USER_REGISTERED,
                                "1.0",
                                2L,
                                Instant.parse(
                                        "2026-09-04T05:00:00Z"),
                                Instant.parse(
                                        "2026-09-04T05:00:00Z"),
                                new Producer(
                                        ProducerType.DEVICE,
                                        "DEVICE-001",
                                        "BOOT-001"),
                                "AGENCY-001",
                                "CORR-002",
                                null),
                        new TapReceivedData(
                                "TOKEN-1",
                                FareMediaType.TRANSIT_CARD,
                                TapType.TAP_IN,
                                "STOP-1",
                                null,
                                null,
                                null,
                                null));

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        httpRequest(
                                EventJson.toJson(event)),
                        null);

        assertEquals(
                400,
                response.getStatusCode());
    }

    @Test
    void shouldRejectMalformedJson() {

        EventPublisher publisher =
                (rawEvent, header) -> {
                    throw new AssertionError(
                            "Publisher must not be called");
                };

        IngestionLambdaHandler handler =
                new IngestionLambdaHandler(publisher);

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        httpRequest("{not-json"),
                        null);

        assertEquals(
                400,
                response.getStatusCode());
    }

    @Test
    void shouldReturnServiceUnavailableWhenSqsPublishFails()
            throws Exception {

        EventPublisher publisher =
                (rawEvent, header) -> {
                    throw new RuntimeException(
                            "simulated SQS failure");
                };

        IngestionLambdaHandler handler =
                new IngestionLambdaHandler(publisher);

        APIGatewayV2HTTPResponse response =
                handler.handleRequest(
                        httpRequest(
                                EventJson.toJson(
                                        validTapReceived())),
                        null);

        assertEquals(
                503,
                response.getStatusCode());
    }

    private static APIGatewayV2HTTPEvent httpRequest(
            String body) {

        APIGatewayV2HTTPEvent request =
                new APIGatewayV2HTTPEvent();

        request.setBody(body);
        request.setIsBase64Encoded(false);

        return request;
    }

    private static EventEnvelope<TapReceivedData>
    validTapReceived() {

        EventHeader header =
                new EventHeader(
                        "EVENT-1",
                        EventType.TAP_RECEIVED,
                        "1.0",
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
                        null);

        TapReceivedData data =
                new TapReceivedData(
                        "TOKEN-0001",
                        FareMediaType.TRANSIT_CARD,
                        TapType.TAP_IN,
                        "STOP-143",
                        "ROUTE-27",
                        "TRIP-8823",
                        "VEHICLE-192",
                        17);

        return new EventEnvelope<>(
                header,
                data);
    }
}
