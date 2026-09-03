# Transit Ticketing Device Simulator

Generates canonical transit events using the shared `event-contracts` module.

Implemented events:

- `TAP_RECEIVED`
- `DEVICE_HEARTBEAT_REPORTED`

The simulator keeps per-device state, including a shared sequence number and cumulative tap/error counters. Heartbeats therefore reflect taps actually produced by the simulator rather than random totals.

## Prerequisites

- Java 21
- Maven 3.9+
- `event-contracts` installed into the local Maven repository

From the repository root on Windows PowerShell:

```powershell
cd C:\Users\mannu\github_repo\transit-event-processing-platform\contracts\event-contracts
mvn clean install
```

Then build the simulator:

```powershell
cd C:\Users\mannu\github_repo\transit-event-processing-platform\services\device-simulator
mvn clean test
```

## Run

```powershell
mvn exec:java
```

Example with options:

```powershell
mvn exec:java -Dexec.args="--devices=5 --passengers=50 --taps=100 --heartbeat-every=10 --sleep-ms=0 --rejection-rate=0.02"
```

Each event is emitted as one compact JSON line to stdout so it can later be redirected to an HTTP publisher, test harness, file, or ingestion endpoint.

## Supported options

```text
--devices=<count>             default 5
--passengers=<count>          default 100
--taps=<count>                default 100
--heartbeat-every=<count>     default 20 taps per device
--sleep-ms=<milliseconds>     default 0
--rejection-rate=<0..1>       default 0.02
--agency=<agency-id>          default AGENCY-001
```

## Design notes

- `eventId`: generated as UUIDv7.
- `sequenceNumber`: monotonic per `producerId + producerInstanceId` and shared across tap + heartbeat event types.
- `producerInstanceId`: unique boot ID per simulated device.
- `totalTaps`, `acceptedTaps`, `rejectedTaps`: cumulative per device.
- `gateCycles`: increments for locally accepted taps.
- Transit context is loaded from `sample-transit-records.csv`. This is a small local fixture shaped around the GTFS/GTFS-RT fields used by the platform contract; it is not a copy of a public dataset.
