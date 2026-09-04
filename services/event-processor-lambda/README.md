# Event Processor Lambda

SQS consumer for the revised ingress architecture:

```text
API Gateway HTTP API -> SQS -> Event Processor Lambda -> DynamoDB fast path
```

Current milestone:

- consumes SQS batches;
- validates `TAP_RECEIVED`, `TAP_ACCEPTED`, `TAP_REJECTED`, and `DEVICE_HEARTBEAT_REPORTED`;
- uses a functional `Map<EventType, Function<...>>` dispatch registry;
- claims `eventId` atomically in the existing DynamoDB idempotency table;
- treats exact duplicates as success;
- treats same-eventId/different-payload as a conflict;
- returns `ReportBatchItemFailures` compatible responses so only failed SQS records retry.

The next milestone adds business dispatch and the ticketing fast-path tables.
