package com.transit.ticketing.simulator.generator;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventHeader;
import com.transit.ticketing.contracts.EventType;
import com.transit.ticketing.contracts.Producer;
import com.transit.ticketing.contracts.ProducerType;
import com.transit.ticketing.contracts.device.DeviceCounters;
import com.transit.ticketing.contracts.device.DeviceHealth;
import com.transit.ticketing.contracts.device.DeviceHeartbeatReportedData;
import com.transit.ticketing.contracts.device.DeviceStatus;
import com.transit.ticketing.contracts.device.NetworkHealth;
import com.transit.ticketing.simulator.device.DeviceState;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import com.transit.ticketing.simulator.id.UuidV7Generator;

import java.time.Instant;
import java.util.Random;

public final class HeartbeatEventGenerator {

    private final Random random;

    public HeartbeatEventGenerator() {
        this(new Random());
    }

    HeartbeatEventGenerator(Random random) {
        this.random = random;
    }

    public EventEnvelope<DeviceHeartbeatReportedData> generate(
            SimulatedDevice device) {

        DeviceState state = device.state();
        Instant now = Instant.now();
        String eventId = UuidV7Generator.next().toString();

        EventHeader header = new EventHeader(
                eventId,
                EventType.DEVICE_HEARTBEAT_REPORTED,
                "1.0",
                state.nextSequenceNumber(),
                now,
                now,
                new Producer(
                        ProducerType.DEVICE,
                        device.deviceId(),
                        device.bootId()),
                device.agencyId(),
                eventId,
                null);

        DeviceCounters counters = new DeviceCounters(
                state.totalTaps(),
                state.acceptedTaps(),
                state.rejectedTaps(),
                state.gateCycles(),
                state.readerErrors(),
                state.motorErrors(),
                state.communicationErrors());

        double loadFactor = Math.min(1.0, state.totalTaps() / 10_000.0);

        DeviceHealth health = new DeviceHealth(
                between(15.0, 70.0),
                between(30.0, 75.0),
                between(40.0, 62.0),
                between(34.0 + 8.0 * loadFactor, 50.0 + 10.0 * loadFactor),
                between(23.5, 24.5));

        NetworkHealth network = new NetworkHealth(
                true,
                (int) Math.round(between(10.0, 80.0)));

        DeviceHeartbeatReportedData data =
                new DeviceHeartbeatReportedData(
                        state.uptimeSeconds(),
                        counters,
                        health,
                        network,
                        deriveStatus(state));

        return new EventEnvelope<>(header, data);
    }

    private DeviceStatus deriveStatus(DeviceState state) {
        long errors = state.readerErrors()
                + state.motorErrors()
                + state.communicationErrors();

        if (errors >= 20) {
            return DeviceStatus.FAULTED;
        }
        if (errors >= 5) {
            return DeviceStatus.DEGRADED;
        }
        return DeviceStatus.HEALTHY;
    }

    private double between(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
}
