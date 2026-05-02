package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Потокобезопасное in-memory хранилище полного runtime state.
 */
public final class InMemoryDetectorRuntimeStateStore implements DetectorRuntimeStateStore {
    private final Map<DetectorInstanceKey, DetectorRuntimeState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<DetectorRuntimeState> get(DetectorInstanceKey key) {
        return Optional.ofNullable(states.get(key));
    }

    @Override
    public void put(DetectorInstanceKey key, DetectorRuntimeState state) {
        states.put(key, state);
    }

    @Override
    public DetectorRuntimeState update(
            DetectorInstanceKey key,
            Supplier<DetectorRuntimeState> initialState,
            UnaryOperator<DetectorRuntimeState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        return states.compute(key, (ignored, current) -> {
            DetectorRuntimeState effectiveCurrent = current == null ? initialState.get() : current;
            return DriftGuardErrors.requireNonNull(transition.apply(effectiveCurrent), "updatedRuntimeState");
        });
    }
}
