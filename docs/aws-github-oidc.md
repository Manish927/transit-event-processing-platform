# GitHub Actions -> AWS with OIDC

Use OIDC instead of storing long-lived AWS access keys in GitHub.

1. Create the GitHub repository first.
2. Configure the GitHub OIDC provider in AWS IAM (`token.actions.githubusercontent.com`, audience `sts.amazonaws.com`).
3. Create an IAM role whose trust policy is restricted to the exact repository/branch OIDC subject claim.
4. Grant that role only the permissions needed to deploy the CloudFormation stack.
5. Add repository variables:
   - `AWS_DEPLOY_ROLE_ARN`
   - `AWS_REGION` (for example `ap-south-1`)
6. Run `.github/workflows/deploy-foundation.yml`.

Important: repositories created after GitHub's 2026 immutable OIDC subject-claim change can have repository/org IDs embedded in the `sub` claim. Use the exact claim emitted for the repository instead of assuming an older owner/repository-only subject format.
