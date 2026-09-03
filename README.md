# Transit Event Processing Platform

A hands-on Java/AWS reference platform for high-volume transit/device event ingestion, idempotent processing, auditing, analytics, and later Apache Flink stream processing.

## Phase 0

- Git repository and project structure
- AWS cost guardrails
- CloudFormation foundation stack
- GitHub Actions deployment skeleton using AWS OIDC

## Phase 1 foundation resources

- Amazon S3 raw-event bucket
- Amazon SQS event queue + dead-letter queue
- Amazon DynamoDB idempotency table
- Amazon SNS notification topic
- CloudWatch log group

The first stack intentionally avoids EC2, RDS, customer-managed KMS keys, and Athena queries so the initial deployment has the lowest possible cost risk. Those services are added in later phases when needed.

## Repository structure

```text
transit-event-processing-platform/
├── .github/workflows/
│   └── deploy-foundation.yml
├── docs/
│   ├── architecture.md
│   └── aws-github-oidc.md
├── infra/cloudformation/
│   └── foundation.yaml
├── scripts/
│   ├── deploy-foundation.sh
│   └── validate-foundation.sh
└── services/
    └── device-publisher/
```

## Local Git setup

```bash
git init
git add .
git commit -m "chore: bootstrap transit event processing platform"
```

## Deploy foundation stack

```bash
export AWS_REGION=ap-south-1
aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name transit-event-dev \
  --template-file infra/cloudformation/foundation.yaml \
  --parameter-overrides Environment=dev ProjectName=transit-event
```

## Next phase

Build the Spring Boot device-event replay service and add API Gateway -> Java Lambda -> SQS -> Java Lambda -> DynamoDB/S3.
