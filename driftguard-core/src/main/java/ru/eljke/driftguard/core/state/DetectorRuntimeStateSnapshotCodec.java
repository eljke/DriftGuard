package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Codec между runtime-состоянием и переносимым snapshot-ом.
 *
 * <p>Этот класс не выбирает физический формат хранения. Он только раскладывает
 * {@link DetectorRuntimeState} на стабильную структуру и восстанавливает его обратно,
 * используя registry codec-ов конкретных detector state.</p>
 */
public final class DetectorRuntimeStateSnapshotCodec {
    private final DetectorStateCodecRegistry detectorStateCodecs;

    public DetectorRuntimeStateSnapshotCodec(DetectorStateCodecRegistry detectorStateCodecs) {
        this.detectorStateCodecs = DriftGuardErrors.requireNonNull(detectorStateCodecs, "detectorStateCodecs");
    }

    public DetectorRuntimeStateSnapshot serialize(DetectorRuntimeState state) {
        DetectorRuntimeState nonNullState = DriftGuardErrors.requireNonNull(state, "state");
        DetectorState detectorState = nonNullState.detectorState();
        DetectorStateCodec<?> detectorStateCodec = detectorStateCodecs.findByState(detectorState)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No detector state codec registered for algorithm: " + detectorState.algorithm()
                ));
        EmissionState emissionState = nonNullState.emissionState();
        return new DetectorRuntimeStateSnapshot(
                nonNullState.schemaVersion(),
                detectorState.algorithm(),
                detectorStateCodec.serializeState(detectorState),
                emissionState.consecutiveSignals(),
                emissionState.lastEmittedAt(),
                emissionState.activeEpisode(),
                emissionState.consecutiveNormal(),
                emissionState.lastEmittedEvent(),
                nonNullState.version()
        );
    }

    public DetectorRuntimeState deserialize(DetectorRuntimeStateSnapshot snapshot) {
        DetectorRuntimeStateSnapshot nonNullSnapshot = DriftGuardErrors.requireNonNull(snapshot, "snapshot");
        DetectorRuntimeStateSchema.requireSupported(nonNullSnapshot.schemaVersion());
        DetectorStateCodec<?> detectorStateCodec = detectorStateCodecs.findByAlgorithm(nonNullSnapshot.algorithm())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No detector state codec registered for algorithm: " + nonNullSnapshot.algorithm()
                ));
        DetectorState detectorState = detectorStateCodec.deserialize(nonNullSnapshot.detectorStatePayload());
        EmissionState emissionState = new EmissionState(
                nonNullSnapshot.consecutiveSignals(),
                nonNullSnapshot.lastEmittedAt(),
                nonNullSnapshot.activeEpisode(),
                nonNullSnapshot.consecutiveNormal(),
                nonNullSnapshot.lastEmittedEvent()
        );
        return DetectorRuntimeState.restore(
                nonNullSnapshot.schemaVersion(),
                detectorState,
                emissionState,
                nonNullSnapshot.version()
        );
    }
}
