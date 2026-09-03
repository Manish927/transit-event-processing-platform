# Architecture Roadmap

## Foundation

```text
GitHub -> GitHub Actions/OIDC -> AWS CloudFormation
                                  |
                                  +-> S3 raw-event bucket
                                  +-> SQS event queue -> SQS DLQ
                                  +-> DynamoDB idempotency table
                                  +-> SNS topic
                                  +-> CloudWatch log group
```

## Target event path

```text
ML dataset
   |
Spring Boot device publisher
   |
API Gateway
   |
Ingestion Lambda (Java)
   |
SQS
   |
Processing Lambda (Java)
   +--> DynamoDB idempotency
   +--> RDS business ledger
   +--> S3 audit/raw data
   +--> SNS notifications

S3 --> Athena
Kafka/Kinesis --> Flink --> grouped/windowed analytics
```
