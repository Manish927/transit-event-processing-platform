# Transit Event Processing Platform

A hands-on Java 21 / AWS reference platform for high-volume public-transit fare and device-event ingestion, asynchronous processing, idempotency, backpressure, queue isolation, transactional processing, and later stream analytics with Kinesis and Apache Flink.

The project is being built incrementally as a production-style architecture implementation.

---

## Current architecture

```text
                         Device Simulator
                              |
                       HttpEventPublisher
                              |
                         API Gateway
                              |
                +-------------+-------------+
                |                           |
        POST /events/fare           POST /events/device
                |                           |
                v                           v
           Fare Event SQS              Device Event SQS
                |                           |
        Event Source Mapping               |
                |                           |
                v                           |
       Event Processor Lambda              |
                |                           |
                v                           |
        DynamoDB Idempotency               |
                                            |
                                  Device processor later
```

### Fare-event flow

```text
Device Simulator
    -> HttpEventPublisher
    -> API Gateway /events/fare
    -> SQS EventQueue
    -> Lambda Event Source Mapping
    -> Event Processor Lambda
    -> Event validation
    -> DynamoDB idempotency
```

### Device-event flow

```text
Device Simulator
    -> HttpEventPublisher
    -> API Gateway /events/device
    -> DeviceEventQueue
```

The device queue is intentionally isolated from fare traffic so telemetry bursts cannot consume the same queue capacity and processing concurrency used by passenger transactions.

---

## Important interaction model

The platform separates the immediate passenger access decision from asynchronous event processing.

```text
Physical tap
    |
    +--> local synchronous ACCEPT / REJECT
    |
    +--> asynchronous event publication
            -> API Gateway
            -> SQS
            -> Lambda
```

An HTTP `202 Accepted` response from the ingestion API means the event was accepted into the asynchronous processing pipeline. It does **not** mean passenger travel authorization was granted.

---

## Implemented event contracts

The common event envelope contains:

```json
{
  "header": {
    "eventId": "...",
    "eventType": "...",
    "schemaVersion": "1.0",
    "sequenceNumber": 1,
    "occurredAt": "...",
    "publishedAt": "...",
    "producer": {
      "producerType": "DEVICE",
      "producerId": "...",
      "producerInstanceId": "..."
    },
    "agencyId": "...",
    "correlationId": "...",
    "causationId": null
  },
  "data": {}
}
```

Implemented event types include:

- `TAP_RECEIVED`
- `TAP_ACCEPTED`
- `TAP_REJECTED`
- `DEVICE_HEARTBEAT_REPORTED`

The contracts also provide the foundation for later customer, account, journey, fare, ledger, device-health, GTFS, and analytics events.

---

## Completed milestones

- Event contracts
- Device simulator
- Local tap decision simulation
- `TAP_RECEIVED`
- `TAP_ACCEPTED`
- `TAP_REJECTED`
- `DEVICE_HEARTBEAT_REPORTED`
- SQS backpressure boundary
- SQS dead-letter queue
- Lambda Event Source Mapping
- Java 21 Event Processor Lambda
- Event validation
- DynamoDB idempotency
- Duplicate-delivery handling
- Partial batch failure support
- GitHub Actions CI/CD
- AWS OIDC authentication
- CloudFormation deployment roles
- Dedicated Device SQS queue
- Dedicated Device DLQ
- API Gateway HTTP API
- `POST /events/fare`
- `POST /events/device`
- Direct API Gateway -> SQS integration
- `HttpEventPublisher`
- End-to-end fare-event verification
- Fare/device traffic isolation

---

## Idempotency and duplicate delivery

Amazon SQS Standard provides at-least-once delivery, so consumers must assume that the same event can be delivered more than once.

Each event has a unique `eventId`.

The Event Processor uses a conditional DynamoDB write to claim an event:

```text
First delivery
    -> conditional PutItem
    -> claim = NEW
    -> processing allowed

Duplicate delivery
    -> conditional PutItem fails
    -> existing payloadHash checked
    -> claim = DUPLICATE
    -> duplicate side effects skipped
```

Verified DynamoDB records contain fields such as:

```text
eventId
eventType
receivedAt
payloadHash
processingStatus = VALIDATED
expiresAt
```

---

## SQS consumer model

Multiple Lambda execution environments consume from the same fare queue as competing workers.

```text
                    SQS
                     |
             Event Source Mapping
                     |
          +----------+----------+
          |          |          |
       Lambda 1   Lambda 2   Lambda 3
```

Message distribution is managed by SQS and the Lambda Event Source Mapping. The application does not manually assign messages to Lambda instances.

Current development configuration uses bounded concurrency to protect downstream DynamoDB capacity.

For independent business consumers that each need their own copy of an event, separate queues or a fan-out/streaming mechanism will be used rather than attaching unrelated consumers to the same SQS queue.

---

## API Gateway routes

### Fare events

```text
POST /events/fare
```

Used for:

- `TAP_RECEIVED`
- `TAP_ACCEPTED`
- `TAP_REJECTED`

Flow:

```text
API Gateway
    -> EventQueue
    -> Event Processor Lambda
    -> DynamoDB
```

### Device events

```text
POST /events/device
```

Used for:

- `DEVICE_HEARTBEAT_REPORTED`
- later device fault/status events

Flow:

```text
API Gateway
    -> DeviceEventQueue
```

A separate Device Processor Lambda will be introduced later.

---

## Device simulator publishing modes

The simulator supports two publishers through the `EventPublisher` abstraction.

### Console mode

```text
ConsoleEventPublisher
```

Events are printed locally.

### HTTP mode

```text
HttpEventPublisher
```

Routing:

```text
TAP_*    -> POST /events/fare
DEVICE_* -> POST /events/device
```

PowerShell example:

```powershell
$API_ENDPOINT = aws cloudformation describe-stacks `
  --stack-name transit-event-dev-api-ingress `
  --region ap-south-1 `
  --query "Stacks[0].Outputs[?OutputKey=='EventIngressApiEndpoint'].OutputValue | [0]" `
  --output text

$env:SIMULATOR_PUBLISHER = "http"
$env:TRANSIT_EVENT_API_ENDPOINT = $API_ENDPOINT

mvn --file services/device-simulator/pom.xml exec:java
```

Expected output includes:

```text
Published eventId=... eventType=TAP_RECEIVED route=/events/fare status=202
Published eventId=... eventType=TAP_ACCEPTED route=/events/fare status=202
Published eventId=... eventType=DEVICE_HEARTBEAT_REPORTED route=/events/device status=202
```

---

## AWS foundation resources

The current development foundation contains:

- Amazon S3 raw-event bucket
- Amazon SQS fare/event queue
- Amazon SQS fare/event dead-letter queue
- Amazon SQS device-event queue
- Amazon SQS device-event dead-letter queue
- Amazon DynamoDB idempotency table
- Amazon SNS notification topic
- Amazon CloudWatch log group

The S3 raw-event bucket currently exists as infrastructure only. Bronze / Silver / Gold processing and Parquet generation are intentionally parked for a later phase.

---

## Main CloudFormation stacks

```text
transit-event-dev
    Foundation resources

transit-event-dev-deployment-artifacts
    Versioned Lambda deployment artifacts

transit-event-dev-ingestion
    Earlier ingestion Lambda path retained temporarily

transit-event-dev-event-processor
    SQS Event Processor Lambda + Event Source Mapping

transit-event-dev-api-ingress
    API Gateway HTTP API + direct SQS integrations

transit-github-oidc-bootstrap
    GitHub OIDC bootstrap

transit-github-oidc-ingestion-permissions
    Deployment permission extensions
```

---

## CI/CD and security

GitHub Actions authenticates to AWS using OIDC.

```text
GitHub Actions
    -> OIDC
    -> GitHub deployment role
    -> CloudFormation
    -> CloudFormation execution role
```

The pipeline does not require long-lived AWS access keys in GitHub.

The deployment role remains intentionally narrow. CloudFormation receives permissions through a separate execution role.

---

## Repository structure

```text
transit-event-processing-platform/
├── .github/
│   └── workflows/
│       ├── deploy-foundation.yml
│       ├── deploy-ingestion.yml
│       ├── deploy-event-processor.yml
│       └── deploy-api-ingress.yml
│
├── contracts/
│   └── event-contracts/
│
├── infra/
│   ├── cloudformation/
│   │   ├── foundation.yaml
│   │   ├── deployment-artifacts.yaml
│   │   ├── ingestion.yaml
│   │   ├── event-processor.yaml
│   │   ├── api-ingress.yaml
│   │   ├── bootstrap-github-oidc.yaml
│   │   └── bootstrap-ingestion-permissions.yaml
│   └── scripts/
│       └── bootstrap-ingestion-pipeline.ps1
│
└── services/
    ├── device-simulator/
    ├── ingestion-lambda/
    └── event-processor-lambda/
```

---

## Build

### Event contracts

```powershell
mvn --file contracts/event-contracts/pom.xml clean install
```

### Device simulator

```powershell
mvn --file services/device-simulator/pom.xml clean test
```

### Event Processor Lambda

```powershell
mvn --file services/event-processor-lambda/pom.xml clean package
```

---

## Functional verification

### Verify fare processing

```text
Device Simulator
    -> API Gateway
    -> EventQueue
    -> Event Processor Lambda
    -> DynamoDB
```

Successful processing produces:

```text
eventType=TAP_RECEIVED
claim=NEW
```

and a DynamoDB item with:

```text
processingStatus=VALIDATED
```

Sending the same event again produces:

```text
claim=DUPLICATE
```

### Verify device routing

```text
Device Simulator
    -> API Gateway /events/device
    -> DeviceEventQueue
```

Because a Device Processor Lambda is not yet attached, heartbeat messages remain available in the device queue for inspection.

---

## Current roadmap

### Next

1. Spring Boot back-office service
   - users
   - accounts
   - funding
   - fare media

2. Functional Java fare-engine library

3. Journey + account + ledger processing

4. DynamoDB transactions and optimistic concurrency

5. Kinesis

6. Apache Flink

### Parked for later

- S3 Bronze / Silver / Gold
- Parquet generation
- long-term raw-event/data-lake design

---

## Planned business flow

```text
Register user
    -> create prepaid account
    -> fund account
    -> link fare media
    -> passenger taps
    -> validate account/media
    -> start/complete journey
    -> calculate fare
    -> debit account
    -> write ledger entry
```

The account debit and ledger update will later use DynamoDB transactions plus optimistic concurrency/versioning to prevent double debit and lost updates.

---

## Planned streaming architecture

```text
SQS
    -> transactional processing
    -> DynamoDB fast path
    -> Kinesis
    -> Apache Flink
    -> later S3 Bronze / Silver / Gold
```

SQS and Kinesis serve different purposes:

- **SQS** provides work-queue semantics, backpressure, retries, and transactional processing isolation.
- **Kinesis** will provide retained streaming data for multiple analytical consumers, replay, ordering, and Flink processing.

---

## Technology stack

- Java 21
- Maven
- AWS Lambda
- Amazon API Gateway HTTP API
- Amazon SQS
- Amazon DynamoDB
- Amazon S3
- Amazon SNS
- Amazon CloudWatch
- AWS CloudFormation
- GitHub Actions
- GitHub OIDC
- Spring Boot — next phase
- Amazon Kinesis — planned
- Apache Flink — planned

---

## Project goal

The goal is to build a realistic, scalable public-transit event-processing platform demonstrating:

- high-volume event ingestion
- asynchronous backpressure
- idempotent processing
- event-driven architecture
- queue isolation
- Java concurrency and functional programming
- Spring Boot business APIs
- optimistic concurrency
- transactional ledger processing
- streaming analytics
- production-style AWS infrastructure and CI/CD
