package com.transit.ticketing.simulator.id;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Minimal UUIDv7 generator using the current Unix epoch milliseconds plus random bits.
 * Suitable for simulation and event identifiers without requiring a third-party UUID library.
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    public static UUID next() {
        long timestampMillis = System.currentTimeMillis();

        long randomA = RANDOM.nextInt(1 << 12);
        long randomB = RANDOM.nextLong();

        long mostSignificantBits =
                ((timestampMillis & 0xFFFFFFFFFFFFL) << 16)
                        | 0x7000L
                        | randomA;

        long leastSignificantBits =
                (randomB & 0x3FFFFFFFFFFFFFFFL)
                        | 0x8000000000000000L;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
