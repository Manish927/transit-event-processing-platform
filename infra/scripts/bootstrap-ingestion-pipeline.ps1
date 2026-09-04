param(
    [string]$Region = "ap-south-1",
    [string]$BootstrapStack = "transit-github-oidc-bootstrap",
    [string]$PermissionStack = "transit-github-oidc-ingestion-permissions"
)

$ErrorActionPreference = "Stop"

$GitHubRoleName = aws cloudformation describe-stack-resource `
    --stack-name $BootstrapStack `
    --logical-resource-id GitHubActionsDeploymentRole `
    --region $Region `
    --query "StackResourceDetail.PhysicalResourceId" `
    --output text

$ExecutionRoleName = aws cloudformation describe-stack-resource `
    --stack-name $BootstrapStack `
    --logical-resource-id CloudFormationExecutionRole `
    --region $Region `
    --query "StackResourceDetail.PhysicalResourceId" `
    --output text

if ([string]::IsNullOrWhiteSpace($GitHubRoleName) -or $GitHubRoleName -eq "None") {
    throw "Could not resolve GitHubActionsDeploymentRole."
}

if ([string]::IsNullOrWhiteSpace($ExecutionRoleName) -or $ExecutionRoleName -eq "None") {
    throw "Could not resolve CloudFormationExecutionRole."
}

Write-Host "GitHub deployment role: $GitHubRoleName"
Write-Host "CloudFormation execution role: $ExecutionRoleName"

aws cloudformation deploy `
    --stack-name $PermissionStack `
    --template-file infra/cloudformation/bootstrap-ingestion-permissions.yaml `
    --parameter-overrides `
      GitHubDeploymentRoleName=$GitHubRoleName `
      CloudFormationExecutionRoleName=$ExecutionRoleName `
      ProjectName=transit-event `
      Environment=dev `
    --capabilities CAPABILITY_NAMED_IAM `
    --region $Region

if ($LASTEXITCODE -ne 0) {
    throw "Permission bootstrap failed."
}

Write-Host "Permission bootstrap deployed successfully."
