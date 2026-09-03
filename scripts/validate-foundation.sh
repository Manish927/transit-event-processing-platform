#!/usr/bin/env bash
set -euo pipefail
AWS_REGION="${AWS_REGION:-ap-south-1}"
aws cloudformation validate-template \
  --region "$AWS_REGION" \
  --template-body file://infra/cloudformation/foundation.yaml
