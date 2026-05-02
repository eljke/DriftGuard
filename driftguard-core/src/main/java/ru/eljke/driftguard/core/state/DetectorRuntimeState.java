package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Единый runtime snapshot detector instance.
 *
 * <p>Detector state и emission state должны сохраняться вместе, иначе при
 * рестарте, replay или переносе partition-а можно получить опубликованные
 * события без соответствующего состояния алгоритма или наоборот.</p>
 *
 * @param detectorState состояние алгоритма
 * @param emissionState состояние emission-политики
 * @param version монотонная версия snapshot-а внутри конкретного хранилища
 */
public record DetectorRuntimeState(
        DetectorState detectorState,
        EmissionState emissionState,
        long version
) {
    public DetectorRuntimeState {
        DriftGuardErrors.requireNonNull(detectorState, "detectorState");
        DriftGuardErrors.requireNonNull(emissionState, "emissionState");
        if (version < 0) {
            throw new IllegalArgumentException("version must be non-negative");
        }
    }

    public static DetectorRuntimeState initial(DetectorState detectorState) {
        return new DetectorRuntimeState(detectorState, EmissionState.EMPTY, 0);
    }

    public DetectorRuntimeState withDetectorState(DetectorState nextDetectorState) {
        return new DetectorRuntimeState(nextDetectorState, emissionState, version + 1);
    }

    public DetectorRuntimeState withEmissionState(EmissionState nextEmissionState) {
        return new DetectorRuntimeState(detectorState, nextEmissionState, version + 1);
    }

    public DetectorRuntimeState advance(DetectorState nextDetectorState, EmissionState nextEmissionState) {
        return new DetectorRuntimeState(nextDetectorState, nextEmissionState, version + 1);
    }
}
