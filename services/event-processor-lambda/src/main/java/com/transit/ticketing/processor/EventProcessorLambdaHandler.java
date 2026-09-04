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
                log(context,
                        "Failed messageId=%s error=%s: %s".formatted(
                                message.getMessageId(),
                                exception.getClass().getSimpleName(),
                                exception.getMessage()));

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

        log(context,
                "Validated eventId=%s eventType=%s messageId=%s claim=%s".formatted(
                        event.header().eventId(),
                        event.header().eventType(),
                        message.getMessageId(),
                        result));

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

    private static void log(Context context, String message) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log(message + System.lineSeparator());
        }
    }
}
