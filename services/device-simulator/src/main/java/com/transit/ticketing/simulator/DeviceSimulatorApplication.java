package com.transit.ticketing.simulator;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.device.DeviceHeartbeatReportedData;
import com.transit.ticketing.contracts.tap.TapReceivedData;
import com.transit.ticketing.contracts.tap.TapType;
import com.transit.ticketing.simulator.config.SimulatorProperties;
import com.transit.ticketing.simulator.dataset.TransitDatasetLoader;
import com.transit.ticketing.simulator.dataset.TransitRecord;
import com.transit.ticketing.simulator.device.DeviceRegistry;
import com.transit.ticketing.simulator.device.SimulatedDevice;
import com.transit.ticketing.simulator.generator.HeartbeatEventGenerator;
import com.transit.ticketing.simulator.generator.TapEventGenerator;
import com.transit.ticketing.simulator.publisher.ConsoleEventPublisher;
import com.transit.ticketing.simulator.publisher.EventPublisher;

import java.util.List;
import java.util.Random;

public final class DeviceSimulatorApplication {

    private DeviceSimulatorApplication() {
    }

    public static void main(String[] args) throws Exception {
        SimulatorProperties properties = SimulatorProperties.fromArgs(args);

        List<TransitRecord> transitRecords =
                new TransitDatasetLoader()
                        .loadFromClasspath("sample-transit-records.csv");

        List<SimulatedDevice> devices =
                DeviceRegistry.create(
                        properties.deviceCount(),
                        properties.agencyId());

        TapEventGenerator tapGenerator = new TapEventGenerator();
        HeartbeatEventGenerator heartbeatGenerator = new HeartbeatEventGenerator();
        EventPublisher publisher = new ConsoleEventPublisher();
        Random random = new Random();

        System.err.printf(
                "Starting simulator: devices=%d passengers=%d taps=%d heartbeatEvery=%d rejectionRate=%.4f%n",
                properties.deviceCount(),
                properties.passengerCount(),
                properties.totalTaps(),
                properties.heartbeatEveryTaps(),
                properties.rejectionRate());

        for (int i = 0; i < properties.totalTaps(); i++) {
            SimulatedDevice device = devices.get(i % devices.size());
            TransitRecord transitRecord = transitRecords.get(i % transitRecords.size());

            int passengerNumber = (i % properties.passengerCount()) + 1;
            String credentialToken = "TOKEN-%08d".formatted(passengerNumber);

            long passengerCycle = i / properties.passengerCount();
            TapType tapType = (passengerCycle % 2 == 0)
                    ? TapType.TAP_IN
                    : TapType.TAP_OUT;

            EventEnvelope<TapReceivedData> tapEvent = tapGenerator.generate(
                    device,
                    transitRecord,
                    credentialToken,
                    tapType);

            publisher.publish(tapEvent);

            boolean locallyAccepted = random.nextDouble() >= properties.rejectionRate();
            device.state().recordTap(locallyAccepted);

            maybeInjectSyntheticError(device, random);

            if (device.state().totalTaps() % properties.heartbeatEveryTaps() == 0) {
                EventEnvelope<DeviceHeartbeatReportedData> heartbeat =
                        heartbeatGenerator.generate(device);
                publisher.publish(heartbeat);
            }

            if (properties.sleepMillis() > 0) {
                Thread.sleep(properties.sleepMillis());
            }
        }

        // Emit a final heartbeat for every device so the last counters are observable.
        for (SimulatedDevice device : devices) {
            publisher.publish(heartbeatGenerator.generate(device));
        }

        System.err.println("Simulation complete.");
    }

    private static void maybeInjectSyntheticError(
            SimulatedDevice device,
            Random random) {

        double value = random.nextDouble();

        if (value < 0.0005) {
            device.state().recordMotorError();
        } else if (value < 0.0020) {
            device.state().recordReaderError();
        } else if (value < 0.0030) {
            device.state().recordCommunicationError();
        }
    }
}
