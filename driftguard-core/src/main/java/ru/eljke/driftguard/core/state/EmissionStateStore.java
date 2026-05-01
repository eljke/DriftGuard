package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.EmissionState;

import java.util.Optional;

/**
 * Хранилище состояния emission-политик.
 */
public interface EmissionStateStore {
    Optional<EmissionState> get(DetectorInstanceKey key);

    void put(DetectorInstanceKey key, EmissionState state);
}
