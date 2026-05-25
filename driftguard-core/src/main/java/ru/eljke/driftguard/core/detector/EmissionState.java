package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;
import java.util.Optional;

/**
 * Emission policy state for one detector instance.
 *
 * @param consecutiveSignals number of consecutive signals from the algorithm
 * @param lastEmittedAt time of the last public event
 * @param activeEpisode whether a drift episode with a published event is active
 * @param consecutiveNormal number of consecutive normal points after the last signal
 * @param lastEmittedEvent last published event of the current episode
 */
public record EmissionState(
        int consecutiveSignals,
        Instant lastEmittedAt,
        boolean activeEpisode,
        int consecutiveNormal,
        DriftEvent lastEmittedEvent
) {
    public static final EmissionState EMPTY = new EmissionState(0, null, false, 0, null);

    public EmissionState(int consecutiveSignals, Instant lastEmittedAt) {
        this(consecutiveSignals, lastEmittedAt, false, 0, null);
    }

    public EmissionState(int consecutiveSignals, Instant lastEmittedAt, boolean activeEpisode, int consecutiveNormal) {
        this(consecutiveSignals, lastEmittedAt, activeEpisode, consecutiveNormal, null);
    }

    public Optional<Instant> lastEmittedAtValue() {
        return Optional.ofNullable(lastEmittedAt);
    }

    public Optional<DriftEvent> lastEmittedEventValue() {
        return Optional.ofNullable(lastEmittedEvent);
    }
}

