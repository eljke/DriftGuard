package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Storage abstraction for detector states.
 *
 * <p>Core provides an in-memory implementation. Kafka Streams should adapt
 * this contract to a state store, while other environments can use databases,
 * embedded caches or temporary storage for replay scenarios.</p>
 */
public interface DetectorStateStore {
    /**
     * Reads state for a metric/detector pair.
     */
    Optional<DetectorState> get(DetectorInstanceKey key);

    /**
     * Saves the latest state for a metric/detector pair.
     */
    void put(DetectorInstanceKey key, DetectorState state);

    /**
     * Atomically reads, modifies and saves detector state.
     *
     * <p>This is the main contract used by the runtime pipeline. Simple stores
     * can inherit the synchronized fallback, while production stores should
     * override the method with a native atomic operation such as compute,
     * transaction, compare-and-set or a state-store update.</p>
     */
    default DetectorState update(
            DetectorInstanceKey key,
            Supplier<? extends DetectorState> initialState,
            UnaryOperator<DetectorState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        synchronized (this) {
            DetectorState current = get(key).orElseGet(initialState);
            DetectorState updated = DriftGuardErrors.requireNonNull(transition.apply(current), "updatedState");
            put(key, updated);
            return updated;
        }
    }
}


