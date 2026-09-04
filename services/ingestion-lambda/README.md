# Transit Ticketing Ingestion Lambda

Thin ingress boundary for transit events.

## Responsibility

The Lambda:

1. Receives an API Gateway HTTP API v2 request.
2. Decodes the request body when Base64 encoded.
3. Validates the common event envelope.
4. Validates the event-specific V1 payload.
5. Allows only the initial ingress event types:
   - TAP_RECEIVED
   - TAP_ACCEPTED
   - TAP_REJECTED
   - DEVICE_HEARTBEAT_REPORTED
6. Sends the original event JSON unchanged to SQS.
7. Returns HTTP 202 Accepted.

It deliberately does **not**:

- calculate fares,
- update account balances,
- perform DynamoDB idempotency,
- create journeys,
- run analytics.

Those responsibilities belong downstream of SQS.

## Required environment variable

```text
EVENT_QUEUE_URL
```

## Lambda handler

```text
com.transit.ticketing.ingestion.IngestionLambdaHandler::handleRequest
```

## Build

First install the shared contract module:

```powershell
cd ~/transit-event-processing-platform\contracts\event-contracts
mvn clean install
```

Then:

```powershell
cd ~\transit-event-processing-platform\services\ingestion-lambda
mvn clean test
mvn clean package
```

Expected deployable artifact:

```text
target\ingestion-lambda.jar
```

## Why this Lambda stays small

SQS is the backpressure boundary. This Lambda should do only synchronous
ingress work and enqueue the validated event as quickly as possible.
Business processing is deliberately kept out of the ingestion path.
