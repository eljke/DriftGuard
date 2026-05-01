package ru.eljke.driftguard.core.detector;

import java.time.Instant;
import java.util.Optional;

/**
 * Состояние emission-политики для одного detector instance.
 *
 * @param consecutiveSignals сколько сигналов подряд пришло от алгоритма
 * @param lastEmittedAt момент последнего публичного события
 */
public record EmissionState(
        int consecutiveSignals,
        Instant lastEmittedAt
) {
    public static final EmissionState EMPTY = new EmissionState(0, null);

    public Optional<Instant> lastEmittedAtValue() {
        return Optional.ofNullable(lastEmittedAt);
    }
}
