package com.transit.ticketing.simulator.publisher;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventJson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Publishes simulator events to the transit HTTP API.
 *
 * Routing:
 *   TAP_*    -> POST /events/fare
 *   DEVICE_* -> POST /events/device
 *
 * The HTTP API is asynchronous. A 202 response means the event was accepted
 * by API Gateway and handed to the configured SQS integration. It does not
 * represent a passenger access decision.
 */
public final class HttpEventPublisher implements EventPublisher {

    private static final String FARE_EVENTS_PATH = "/events/fare";
    private static final String DEVICE_EVENTS_PATH = "/events/device";

    private static final String EVENT_ID_HEADER = "X-Event-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String API_GATEWAY_REQUEST_ID_HEADER = "x-amzn-requestid";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final String baseEndpoint;
    private final Duration requestTimeout;

    public HttpEventPublisher(String baseEndpoint) {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                        .build(),
                baseEndpoint,
                DEFAULT_REQUEST_TIMEOUT);
    }

    HttpEventPublisher(
            HttpClient httpClient,
            String baseEndpoint,
            Duration requestTimeout) {

        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.baseEndpoint = normalizeBaseEndpoint(baseEndpoint);
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
    }

    @Override
    public void publish(EventEnvelope<?> event) throws Exception {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(event.header(), "event.header must not be null");

        String eventType = String.valueOf(event.header().eventType());
        String eventId = String.valueOf(event.header().eventId());
        String correlationId = event.header().correlationId() == null
                ? eventId
                : String.valueOf(event.header().correlationId());

        String path = resolvePath(eventType);
        URI target = URI.create(baseEndpoint + path);
        String json = EventJson.toJson(event);

        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(EVENT_ID_HEADER, eventId)
                .header(CORRELATION_ID_HEADER, correlationId)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        final HttpResponse<String> response;
        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }

        String apiRequestId = response.headers()
                .firstValue(API_GATEWAY_REQUEST_ID_HEADER)
                .orElse("-");

        if (response.statusCode() != 202) {
            throw new IOException(
                    "Event publish failed: eventId=%s correlationId=%s eventType=%s route=%s status=%d apiRequestId=%s body=%s"
                            .formatted(
                                    eventId,
                                    correlationId,
                                    eventType,
                                    path,
                                    response.statusCode(),
                                    apiRequestId,
                                    abbreviate(response.body(), 500)));
        }

        System.err.printf(
                "Published eventId=%s correlationId=%s eventType=%s route=%s status=%d apiRequestId=%s%n",
                eventId,
                correlationId,
                eventType,
                path,
                response.statusCode(),
                apiRequestId);
    }

    private static String resolvePath(String eventType) {
        if (eventType.startsWith("TAP_")) {
            return FARE_EVENTS_PATH;
        }

        if (eventType.startsWith("DEVICE_")) {
            return DEVICE_EVENTS_PATH;
        }

        throw new IllegalArgumentException(
                "Unsupported simulator event type for HTTP publishing: " + eventType);
    }

    private static String normalizeBaseEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("API endpoint must not be blank");
        }

        String normalized = endpoint.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "API endpoint must use http or https: " + endpoint);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "API endpoint must contain a valid host: " + endpoint);
        }

        return normalized;
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength) + "...";
    }
}
