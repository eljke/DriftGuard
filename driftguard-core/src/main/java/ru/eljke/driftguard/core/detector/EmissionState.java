package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;
import java.util.Optional;

/**
 * Состояние emission-политики для одного detector instance.
 *
 * @param consecutiveSignals сколько сигналов подряд пришло от алгоритма
 * @param lastEmittedAt момент последнего публичного события
 * @param activeEpisode открыт ли уже drift episode, для которого событие было опубликовано
 * @param consecutiveNormal сколько нормальных точек подряд пришло после последнего сигнала
 * @param lastEmittedEvent последнее опубликованное событие текущего episode
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
