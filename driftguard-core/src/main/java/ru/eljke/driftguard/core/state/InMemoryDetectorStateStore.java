package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Потокобезопасное in-memory хранилище состояний для тестов, локального demo
 * и запусков без долговременного хранения.
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
}
