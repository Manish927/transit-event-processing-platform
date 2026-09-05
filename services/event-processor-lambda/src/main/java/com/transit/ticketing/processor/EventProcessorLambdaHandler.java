package com.transit.ticketing.processor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.processor.idempotency.ClaimResult;
import com.transit.ticketing.processor.idempotency.DynamoDbIdempotencyStore;
import com.transit.ticketing.processor.idempotency.IdempotencyStore;
import com.transit.ticketing.processor.validation.EventValidator;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public final class EventProcessorLambdaHandler
        implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final EventValidator VALIDATOR = new EventValidator();

    private final IdempotencyStore idempotencyStore;

    public EventProcessorLambdaHandler() {
        this(new DynamoDbIdempotencyStore(
                DynamoDbClient.create(),
                requiredEnvironmentVariable("IDEMPOTENCY_TABLE_NAME"),
                Clock.systemUTC(),
                7));
    }

    EventProcessorLambdaHandler(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        if (event == null || event.getRecords() == null) {
            return new SQSBatchResponse(failures);
        }

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                process(message, context);
            } catch (Exception exception) {
                logJson(
                        context,
                        "ERROR",
                        "Event processing failed",
                        messageAttribute(message, "eventId"),
                        messageAttribute(message, "correlationId"),
                        "-",
                        "-",
                        message.getMessageId(),
                        messageAttribute(message, "apiRequestId"),
                        lambdaRequestId(context),
                        "-",
                        exception.getClass().getSimpleName() + ": " + safe(exception.getMessage()));

                failures.add(new SQSBatchResponse.BatchItemFailure(
                        message.getMessageId()));
            }
        }

        return new SQSBatchResponse(failures);
    }

    private void process(SQSEvent.SQSMessage message, Context context) {
        EventEnvelope<?> event = VALIDATOR.validate(message.getBody());

        ClaimResult result = idempotencyStore.claim(
                event.header(),
                message.getBody());

        logJson(
                context,
                "INFO",
                "Event validated",
                String.valueOf(event.header().eventId()),
                event.header().correlationId() == null
                        ? String.valueOf(event.header().eventId())
                        : String.valueOf(event.header().correlationId()),
                String.valueOf(event.header().eventType()),
                event.header().producer() == null
                        ? "-"
                        : String.valueOf(event.header().producer().producerId()),
                message.getMessageId(),
                messageAttribute(message, "apiRequestId"),
                lambdaRequestId(context),
                String.valueOf(result),
                "-");

        /*
         * Next milestone: dispatch NEW events to domain processors that update
         * fare-media/accounts/journeys/ledger transactionally. DUPLICATE events
         * are safely acknowledged and not processed again.
         */
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + name);
        }
        return value;
    }

    private static String messageAttribute(
            SQSEvent.SQSMessage message,
            String name) {

        if (message == null || message.getMessageAttributes() == null) {
            return "-";
        }

        var attribute = message.getMessageAttributes().get(name);
        if (attribute == null
                || attribute.getStringValue() == null
                || attribute.getStringValue().isBlank()) {
            return "-";
        }

        return attribute.getStringValue();
    }

    private static String lambdaRequestId(Context context) {
        if (context == null || context.getAwsRequestId() == null) {
            return "-";
        }
        return context.getAwsRequestId();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static void logJson(
            Context context,
            String level,
            String message,
            String eventId,
            String correlationId,
            String eventType,
            String producerId,
            String sqsMessageId,
            String apiRequestId,
            String lambdaRequestId,
            String claim,
            String error) {

        if (context == null || context.getLogger() == null) {
            return;
        }

        String json = """
                {"level":"%s","message":"%s","eventId":"%s","correlationId":"%s","eventType":"%s","producerId":"%s","sqsMessageId":"%s","apiRequestId":"%s","lambdaRequestId":"%s","claim":"%s","error":"%s"}
                """.formatted(
                jsonEscape(safe(level)),
                jsonEscape(safe(message)),
                jsonEscape(safe(eventId)),
                jsonEscape(safe(correlationId)),
                jsonEscape(safe(eventType)),
                jsonEscape(safe(producerId)),
                jsonEscape(safe(sqsMessageId)),
                jsonEscape(safe(apiRequestId)),
                jsonEscape(safe(lambdaRequestId)),
                jsonEscape(safe(claim)),
                jsonEscape(safe(error)))
                .strip();

        context.getLogger().log(json + System.lineSeparator());
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append("\\u%04x".formatted((int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }

        return escaped.toString();
    }
}
