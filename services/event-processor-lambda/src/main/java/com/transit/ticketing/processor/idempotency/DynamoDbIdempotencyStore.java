package com.transit.ticketing.processor.idempotency;

import com.transit.ticketing.contracts.EventHeader;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

public final class DynamoDbIdempotencyStore implements IdempotencyStore {

    private final DynamoDbClient dynamoDb;
    private final String tableName;
    private final Clock clock;
    private final long ttlDays;

    public DynamoDbIdempotencyStore(
            DynamoDbClient dynamoDb,
            String tableName,
            Clock clock,
            long ttlDays) {

        this.dynamoDb = Objects.requireNonNull(dynamoDb, "dynamoDb must not be null");
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        if (ttlDays <= 0) {
            throw new IllegalArgumentException("ttlDays must be positive");
        }
        this.tableName = tableName;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.ttlDays = ttlDays;
    }

    @Override
    public ClaimResult claim(EventHeader header, String rawEvent) {
        String payloadHash = sha256(rawEvent);
        Instant now = clock.instant();
        long expiresAt = now.plus(ttlDays, ChronoUnit.DAYS).getEpochSecond();

        Map<String, AttributeValue> item = Map.of(
                "eventId", AttributeValue.fromS(header.eventId()),
                "eventType", AttributeValue.fromS(header.eventType().name()),
                "processingStatus", AttributeValue.fromS("VALIDATED"),
                "receivedAt", AttributeValue.fromS(now.toString()),
                "payloadHash", AttributeValue.fromS(payloadHash),
                "expiresAt", AttributeValue.fromN(Long.toString(expiresAt))
        );

        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .conditionExpression("attribute_not_exists(eventId)")
                    .build());
            return ClaimResult.NEW;

        } catch (ConditionalCheckFailedException duplicate) {
            String existingHash = readExistingHash(header.eventId());

            if (payloadHash.equals(existingHash)) {
                return ClaimResult.DUPLICATE;
            }

            throw new IdempotencyConflictException(
                    "eventId already exists with a different payload: " + header.eventId());
        }
    }

    private String readExistingHash(String eventId) {
        var response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.fromS(eventId)))
                .consistentRead(true)
                .projectionExpression("payloadHash")
                .build());

        AttributeValue hash = response.item().get("payloadHash");
        return hash == null ? null : hash.s();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
