package com.transit.ticketing.simulator.publisher;

import com.transit.ticketing.contracts.EventEnvelope;
import com.transit.ticketing.contracts.EventJson;

public final class ConsoleEventPublisher implements EventPublisher {

    @Override
    public synchronized void publish(EventEnvelope<?> event) throws Exception {
        System.out.println(EventJson.toJson(event));
    }
}
