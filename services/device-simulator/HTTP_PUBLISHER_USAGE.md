# Device Simulator HTTP Publisher

The simulator now supports two publishing modes without adding any external HTTP dependency.

## Console mode (default)

```powershell
Remove-Item Env:SIMULATOR_PUBLISHER -ErrorAction SilentlyContinue
mvn --file services/device-simulator/pom.xml exec:java
```

## HTTP mode

Set the API Gateway base endpoint returned by the `transit-event-dev-api-ingress` stack:

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

Routing:
- `TAP_*` -> `POST /events/fare`
- `DEVICE_*` -> `POST /events/device`

A successful publish requires HTTP 202.
