package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.EmissionState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
}
