package com.transit.ticketing.ingestion;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.ingestion.publisher.EventPublisher;
import com.transit.ticketing.ingestion.publisher.SqsEventPublisher;
import com.transit.ticketing.ingestion.validation.EventValidationException;
import com.transit.ticketing.ingestion.validation.EventValidator;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

public final class IngestionLambdaHandler
        implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static final Map<String, String> RESPONSE_HEADERS = Map.of(
            "Content-Type", "application/json",
            "X-Content-Type-Options", "nosniff"
    );

    // Reuse thread-safe, heavy instances across cold starts
    private static final EventValidator VALIDATOR = new EventValidator();

    private final EventPublisher publisher;

    /**
     * AWS Lambda default constructor using optimized CRT HTTP client.
     */
    public IngestionLambdaHandler() {
        this(new SqsEventPublisher(
                SqsClient.builder()
                        .httpClientBuilder(AwsCrtHttpClient.builder())
                        .build(),
                requiredEnvironmentVariable("EVENT_QUEUE_URL")));
    }

    /**
     * Package-private constructor for dependency injection in unit tests.
     */
    IngestionLambdaHandler(EventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "EventPublisher must not be null");
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        if (request == null || isNullOrBlank(request.getBody())) {
            log(context, "WARN", "Rejected request: empty body");
            return buildResponse(400, "{\"status\":\"REJECTED\",\"code\":\"EMPTY_BODY\"}");
        }

        final String rawEvent;
        try {
            rawEvent = decodeBody(request);
        } catch (IllegalArgumentException e) {
            log(context, "WARN", "Rejected request: invalid Base64 body");
            return buildResponse(400, "{\"status\":\"REJECTED\",\"code\":\"INVALID_BODY_ENCODING\"}");
        }

        try {
            EventEnvelope<?> event = VALIDATOR.validate(rawEvent);
            String sqsMessageId = publisher.publish(rawEvent, event.header());

            log(context, "INFO", String.format(
                    "Accepted eventId=%s eventType=%s sqsMessageId=%s",
                    event.header().eventId(),
                    event.header().eventType(),
                    sqsMessageId));

            return buildResponse(202, String.format("{\"status\":\"ACCEPTED\",\"eventId\":\"%s\"}", event.header().eventId()));

        } catch (EventValidationException e) {
            log(context, "WARN", "Rejected event validation failure: " + e.getMessage());
            return buildResponse(400, "{\"status\":\"REJECTED\",\"code\":\"INVALID_EVENT\"}");

        } catch (Exception e) {
            log(context, "ERROR", String.format("Ingestion failure [%s]: %s",
                    e.getClass().getSimpleName(), e.getMessage()));

            return buildResponse(503, "{\"status\":\"FAILED\",\"code\":\"INGESTION_UNAVAILABLE\"}");
        }
    }

    private static String decodeBody(APIGatewayV2HTTPEvent request) {
        if (!Boolean.TRUE.equals(request.getIsBase64Encoded())) {
            return request.getBody();
        }
        byte[] decoded = Base64.getDecoder().decode(request.getBody());
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static APIGatewayV2HTTPResponse buildResponse(int statusCode, String jsonBody) {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(statusCode)
                .withHeaders(RESPONSE_HEADERS)
                .withBody(jsonBody)
                .withIsBase64Encoded(false)
                .build();
    }

    private static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (isNullOrBlank(value)) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    private static void log(Context context, String level, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(String.format("[%s] %s%n", level, message));
        }
    }
}