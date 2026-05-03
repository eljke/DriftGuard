package ru.eljke.driftguard.kafka;

import org.apache.kafka.streams.state.KeyValueStore;
import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;
import ru.eljke.driftguard.core.state.DetectorRuntimeState;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateSnapshot;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateSnapshotCodec;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateStore;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Adapter между core-хранилищем runtime state и Kafka Streams state store.
 *
 * <p>В Kafka state store сохраняется переносимый snapshot, а не Java-объект
 * состояния алгоритма. Это позволяет позже менять физический формат хранения
 * и добавлять миграции схемы без изменения core engine.</p>
 */
public final class KafkaDetectorRuntimeStateStore implements DetectorRuntimeStateStore {
    private final KeyValueStore<String, DetectorRuntimeStateSnapshot> store;
    private final DetectorRuntimeStateSnapshotCodec snapshotCodec;

    public KafkaDetectorRuntimeStateStore(
            KeyValueStore<String, DetectorRuntimeStateSnapshot> store,
            DetectorRuntimeStateSnapshotCodec snapshotCodec
    ) {
        this.store = DriftGuardErrors.requireNonNull(store, "store");
        this.snapshotCodec = DriftGuardErrors.requireNonNull(snapshotCodec, "snapshotCodec");
    }

    @Override
    public Optional<DetectorRuntimeState> get(DetectorInstanceKey key) {
        DetectorRuntimeStateSnapshot snapshot = store.get(KafkaDetectorInstanceKeys.stateKey(key));
        return Optional.ofNullable(snapshot).map(snapshotCodec::deserialize);
    }

    @Override
    public void put(DetectorInstanceKey key, DetectorRuntimeState state) {
        store.put(KafkaDetectorInstanceKeys.stateKey(key), snapshotCodec.serialize(state));
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

        String stateKey = KafkaDetectorInstanceKeys.stateKey(key);
        DetectorRuntimeStateSnapshot currentSnapshot = store.get(stateKey);
        DetectorRuntimeState current = currentSnapshot == null
                ? initialState.get()
                : snapshotCodec.deserialize(currentSnapshot);
        DetectorRuntimeState updated = DriftGuardErrors.requireNonNull(
                transition.apply(current),
                "updatedRuntimeState"
        );
        store.put(stateKey, snapshotCodec.serialize(updated));
        return updated;
    }
}
