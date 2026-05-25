package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Unified runtime snapshot of a detector instance.
 *
 * <p>Detector state and emission state must be persisted together; otherwise during
 * restart, replay or partition migration, the system can get published
 * events without matching algorithm state or the opposite.</p>
 *
 * @param detectorState algorithm state
 * @param emissionState emission policy state
 * @param version monotonic runtime snapshot version inside a concrete store
 */
public record DetectorRuntimeState(
        DetectorState detectorState,
        EmissionState emissionState,
        long version
) {
    public static final int CURRENT_SCHEMA_VERSION = DetectorRuntimeStateSchema.CURRENT_VERSION;

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

    /**
     * Restores runtime state from a persisted snapshot after validating its schema version.
     */
    public static DetectorRuntimeState restore(
            int schemaVersion,
            DetectorState detectorState,
            EmissionState emissionState,
            long version
    ) {
        DetectorRuntimeStateSchema.requireSupported(schemaVersion);
        return new DetectorRuntimeState(detectorState, emissionState, version);
    }

    /**
     * Persisted snapshot schema version. It is independent from the per-key runtime version.
     */
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
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

