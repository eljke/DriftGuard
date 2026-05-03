package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.Arrays;

/**
 * Переносимый snapshot runtime-состояния detector instance.
 *
 * <p>Snapshot не привязан к конкретному формату хранения. Kafka, JDBC или Redis adapter
 * могут сериализовать этот объект в JSON, Avro, Protobuf или другой формат, но структура
 * snapshot-а остаётся общей для всех хранилищ.</p>
 *
 * @param schemaVersion версия схемы persisted snapshot-а
 * @param algorithm имя алгоритма, которому принадлежит detector state
 * @param detectorStatePayload сериализованное состояние алгоритма
 * @param consecutiveSignals количество последовательных сигналов drift-а
 * @param lastEmittedAt момент последнего опубликованного события
 * @param activeEpisode открыт ли episode, для которого уже было событие
 * @param consecutiveNormal количество последовательных нормальных точек после drift-а
 * @param lastEmittedEvent последнее опубликованное событие текущего episode
 * @param version монотонная runtime-версия состояния внутри хранилища
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
