package com.transit.ticketing.simulator.publisher;

import com.transit.ticketing.contracts.EventEnvelope;

public interface EventPublisher {

    void publish(EventEnvelope<?> event) throws Exception;
}
