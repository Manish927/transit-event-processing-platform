package com.transit.ticketing.ingestion.publisher;

import com.transit.ticketing.contracts.EventHeader;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;
import java.util.Objects;

public final class SqsEventPublisher
        implements EventPublisher {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsEventPublisher(
            SqsClient sqsClient,
            String queueUrl) {

        this.sqsClient =
                Objects.requireNonNull(
                        sqsClient,
                        "sqsClient must not be null");

        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "queueUrl must not be blank");
        }

        this.queueUrl = queueUrl;
    }

    @Override
    public String publish(
            String rawEvent,
            EventHeader header) {

        Objects.requireNonNull(
                rawEvent,
                "rawEvent must not be null");

        Objects.requireNonNull(
                header,
                "header must not be null");

        SendMessageRequest request =
                SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(rawEvent)
                        .messageAttributes(
                                Map.of(
                                        "eventType",
                                        stringAttribute(
                                                header.eventType()
                                                        .name()),
                                        "schemaVersion",
                                        stringAttribute(
                                                header.schemaVersion()),
                                        "agencyId",
                                        stringAttribute(
                                                header.agencyId()),
                                        "producerId",
                                        stringAttribute(
                                                header.producer()
                                                        .producerId())))
                        .build();

        SendMessageResponse response =
                sqsClient.sendMessage(request);

        return response.messageId();
    }

    private static MessageAttributeValue stringAttribute(
            String value) {

        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }
}
