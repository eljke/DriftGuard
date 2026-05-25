package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.Arrays;

/**
 * Portable runtime-state snapshot for a detector instance.
 *
 * <p>The snapshot is not bound to a concrete storage format. Kafka, JDBC or Redis adapters
 * can serialize this object as JSON, Avro, Protobuf or another format, while the
 * snapshot structure remains shared by all stores.</p>
 *
 * @param schemaVersion schema version of the persisted snapshot
 * @param algorithm algorithm name that owns the detector state
 * @param detectorStatePayload serialized algorithm state
 * @param consecutiveSignals number of consecutive drift signals
 * @param lastEmittedAt time of the last published event
 * @param activeEpisode whether an already published episode is active
 * @param consecutiveNormal number of consecutive normal points after drift
 * @param lastEmittedEvent last published event of the current episode
 * @param version monotonic runtime state version inside the store
 */
public record DetectorRuntimeStateSnapshot(
        int schemaVersion,
        String algorithm,
        byte[] detectorStatePayload,
        int consecutiveSignals,
        Instant lastEmittedAt,
        boolean activeEpisode,
        int consecutiveNormal,
        DriftEvent lastEmittedEvent,
        long version
) {
    public DetectorRuntimeStateSnapshot {
        DetectorRuntimeStateSchema.requireSupported(schemaVersion);
        algorithm = DriftGuardErrors.requireNonBlank(algorithm, "algorithm");
        detectorStatePayload = Arrays.copyOf(
                DriftGuardErrors.requireNonNull(detectorStatePayload, "detectorStatePayload"),
                detectorStatePayload.length
        );
        if (consecutiveSignals < 0) {
            throw new IllegalArgumentException("consecutiveSignals must be non-negative");
        }
        if (consecutiveNormal < 0) {
            throw new IllegalArgumentException("consecutiveNormal must be non-negative");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must be non-negative");
        }
    }

    @Override
    public byte[] detectorStatePayload() {
        return Arrays.copyOf(detectorStatePayload, detectorStatePayload.length);
    }
}

