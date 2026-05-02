package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * In-memory хранилище состояния emission-политик.
 */
public final class InMemoryEmissionStateStore implements EmissionStateStore {
    private final Map<DetectorInstanceKey, EmissionState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<EmissionState> get(DetectorInstanceKey key) {
        return Optional.ofNullable(states.get(key));
    }

    @Override
    public void put(DetectorInstanceKey key, EmissionState state) {
        states.put(key, state);
    }

    @Override
    public EmissionState update(DetectorInstanceKey key, UnaryOperator<EmissionState> transition) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(transition, "transition");
        return states.compute(key, (ignored, current) -> {
            EmissionState effectiveCurrent = current == null ? EmissionState.EMPTY : current;
            return DriftGuardErrors.requireNonNull(transition.apply(effectiveCurrent), "updatedEmissionState");
        });
    }
}
