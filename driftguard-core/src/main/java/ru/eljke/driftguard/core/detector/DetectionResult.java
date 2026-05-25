package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;

/**
 * Detector output containing the updated algorithm state and an optional drift event.
 *
 * @param state updated detector state
 * @param event optional emitted drift event
 * Detector output containing the updated algorithm state and an optional drift event.
 */
public record DetectionResult<S extends DetectorState>(
        S state,
        DriftEvent event
) {
    public DetectionResult {
        state = DriftGuardErrors.requireNonNull(state, "state");
    }

    public static <S extends DetectorState> DetectionResult<S> noDrift(S state) {
        return new DetectionResult<>(state, null);
    }

    public static <S extends DetectorState> DetectionResult<S> drift(S state, DriftEvent event) {
        return new DetectionResult<>(state, event);
    }

    public Optional<DriftEvent> eventValue() {
        return Optional.ofNullable(event);
    }
}


