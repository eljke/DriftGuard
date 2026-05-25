package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Thread-safe in-memory state store for tests, local demo runs
 * and executions without durable persistence.
 */
public final class InMemoryDetectorStateStore implements DetectorStateStore {
    private final Map<DetectorInstanceKey, DetectorState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<DetectorState> get(DetectorInstanceKey key) {
        return Optional.ofNullable(states.get(key));
    }

    @Override
    public void put(DetectorInstanceKey key, DetectorState state) {
        states.put(key, state);
    }

    @Override
    public DetectorState update(
            DetectorInstanceKey key,
            Supplier<? extends DetectorState> initialState,
            UnaryOperator<DetectorState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        return states.compute(key, (ignored, current) -> {
            DetectorState effectiveCurrent = current == null ? initialState.get() : current;
            return DriftGuardErrors.requireNonNull(transition.apply(effectiveCurrent), "updatedState");
        });
    }
}

