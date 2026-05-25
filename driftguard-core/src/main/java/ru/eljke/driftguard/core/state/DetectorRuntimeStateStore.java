package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Store for full runtime state of one detector instance.
 *
 * <p>This is the main state contract for the processing pipeline. Kafka,
 * JDBC or Redis adapters must persist {@link DetectorRuntimeState} atomically so
 * detector state and emission state cannot diverge.</p>
 */
public interface DetectorRuntimeStateStore {
    Optional<DetectorRuntimeState> get(DetectorInstanceKey key);

    void put(DetectorInstanceKey key, DetectorRuntimeState state);

    /**
     * Atomically reads, modifies and saves the full runtime snapshot.
     */
    default DetectorRuntimeState update(
            DetectorInstanceKey key,
            Supplier<DetectorRuntimeState> initialState,
            UnaryOperator<DetectorRuntimeState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        synchronized (this) {
            DetectorRuntimeState current = get(key).orElseGet(initialState);
            DetectorRuntimeState updated = DriftGuardErrors.requireNonNull(transition.apply(current), "updatedRuntimeState");
            put(key, updated);
            return updated;
        }
    }
}


