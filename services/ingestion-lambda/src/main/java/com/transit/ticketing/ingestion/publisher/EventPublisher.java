package com.transit.ticketing.ingestion.publisher;

import com.transit.ticketing.contracts.EventHeader;

public interface EventPublisher {

    String publish(
            String rawEvent,
            EventHeader header);
}
