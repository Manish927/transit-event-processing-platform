#!/usr/bin/env bash
set -euo pipefail
AWS_REGION="${AWS_REGION:-ap-south-1}"
aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name transit-event-dev \
  --template-file infra/cloudformation/foundation.yaml \
  --parameter-overrides ProjectName=transit-event Environment=dev \
  --no-fail-on-empty-changeset
