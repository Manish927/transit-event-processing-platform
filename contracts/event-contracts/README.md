# Transit Ticketing Event Contracts

Shared Java event contract library for the Transit Event Processing Platform.

## Requirements
- Java 21
- Maven 3.9+

## Build
Run from `contracts/event-contracts`:

```powershell
mvn clean test
```

Initial concrete payloads:
- `TAP_RECEIVED`
- `DEVICE_HEARTBEAT_REPORTED`
