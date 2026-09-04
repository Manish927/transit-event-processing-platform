package com.transit.ticketing.processor.idempotency;

import com.transit.ticketing.contracts.EventHeader;

public interface IdempotencyStore {
    ClaimResult claim(EventHeader header, String rawEvent);
}
