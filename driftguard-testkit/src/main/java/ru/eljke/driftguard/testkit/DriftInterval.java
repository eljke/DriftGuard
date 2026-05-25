package ru.eljke.driftguard.testkit;

import java.time.Instant;
import java.util.Objects;

/**
 * Expected drift time interval in a synthetic scenario.
 *
 * @param start start timestamp
 * @param end end timestamp
 */
public record DriftInterval(
        Instant start,
        Instant end
) {
    public DriftInterval {
        start = Objects.requireNonNull(start, "start must not be null");
        end = Objects.requireNonNull(end, "end must not be null");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must not be before start");
        }
    }

    public boolean contains(Instant timestamp) {
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }
}


