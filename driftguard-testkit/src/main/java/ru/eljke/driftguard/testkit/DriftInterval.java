package ru.eljke.driftguard.testkit;

import java.time.Instant;
import java.util.Objects;

/**
 * Ожидаемый интервал деградации в синтетическом сценарии.
 *
 * @param start момент, с которого поток считается дрейфующим
 * @param end момент окончания drift-интервала
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
