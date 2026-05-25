package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Store for emission policy state.
 */
public interface EmissionStateStore {
    Optional<EmissionState> get(DetectorInstanceKey key);

    void put(DetectorInstanceKey key, EmissionState state);

    /**
     * Atomically reads, modifies and saves emission policy state.
     */
    default EmissionState update(DetectorInstanceKey key, UnaryOperator<EmissionState> transition) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(transition, "transition");
        synchronized (this) {
            EmissionState current = get(key).orElse(EmissionState.EMPTY);
            EmissionState updated = DriftGuardErrors.requireNonNull(transition.apply(current), "updatedEmissionState");
            put(key, updated);
            return updated;
        }
    }
}

