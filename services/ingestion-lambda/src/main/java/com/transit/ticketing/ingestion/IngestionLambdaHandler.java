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
import software.amazon.awssdk.services.sqs.SqsClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class IngestionLambdaHandler
        implements RequestHandler<
        APIGatewayV2HTTPEvent,
        APIGatewayV2HTTPResponse> {

    private static final EventValidator VALIDATOR =
            new EventValidator();

    private final EventPublisher publisher;

    /*
     * AWS Lambda uses this constructor.
     * The SQS client is created once per Lambda execution environment
     * and reused by warm invocations.
     */
    public IngestionLambdaHandler() {
        this(new SqsEventPublisher(
                SqsClient.create(),
                requiredEnvironmentVariable("EVENT_QUEUE_URL")));
    }

    /*
     * Visible for unit tests.
     */
    IngestionLambdaHandler(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(
            APIGatewayV2HTTPEvent request,
            Context context) {

        if (request == null
                || request.getBody() == null
                || request.getBody().isBlank()) {

            log(context, "Rejected request: empty body");

            return response(
                    400,
                    """
                    {"status":"REJECTED","code":"EMPTY_BODY"}
                    """);
        }

        final String rawEvent;

        try {
            rawEvent = decodeBody(request);
        } catch (IllegalArgumentException exception) {
            log(context, "Rejected request: invalid Base64 body");

            return response(
                    400,
                    """
                    {"status":"REJECTED","code":"INVALID_BODY_ENCODING"}
                    """);
        }

        try {
            EventEnvelope<?> event =
                    VALIDATOR.validate(rawEvent);

            String sqsMessageId =
                    publisher.publish(
                            rawEvent,
                            event.header());

            log(
                    context,
                    "Accepted eventId=%s eventType=%s sqsMessageId=%s"
                            .formatted(
                                    event.header().eventId(),
                                    event.header().eventType(),
                                    sqsMessageId));

            return response(
                    202,
                    """
                    {"status":"ACCEPTED","eventId":"%s"}
                    """.formatted(event.header().eventId()).trim());

        } catch (EventValidationException exception) {

            log(
                    context,
                    "Rejected event: " + exception.getMessage());

            return response(
                    400,
                    """
                    {"status":"REJECTED","code":"INVALID_EVENT"}
                    """);

        } catch (Exception exception) {

            /*
             * Do not expose AWS/internal exception details to callers.
             * CloudWatch contains the diagnostic information.
             */
            log(
                    context,
                    "Ingestion failure: "
                            + exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage());

            return response(
                    503,
                    """
                    {"status":"FAILED","code":"INGESTION_UNAVAILABLE"}
                    """);
        }
    }

    private static String decodeBody(
            APIGatewayV2HTTPEvent request) {

        if (!Boolean.TRUE.equals(
                request.getIsBase64Encoded())) {

            return request.getBody();
        }

        byte[] decoded =
                Base64.getDecoder()
                        .decode(request.getBody());

        return new String(
                decoded,
                StandardCharsets.UTF_8);
    }

    private static APIGatewayV2HTTPResponse response(
            int statusCode,
            String body) {

        APIGatewayV2HTTPResponse response =
                new APIGatewayV2HTTPResponse();

        response.setStatusCode(statusCode);
        response.setHeaders(
                Map.of(
                        "content-type",
                        "application/json"));
        response.setBody(body.trim());
        response.setIsBase64Encoded(false);

        return response;
    }

    private static String requiredEnvironmentVariable(
            String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: "
                            + name);
        }

        return value;
    }

    private static void log(
            Context context,
            String message) {

        if (context != null
                && context.getLogger() != null) {

            context.getLogger().log(message + System.lineSeparator());
        }
    }
}
