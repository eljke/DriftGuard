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
 * Detector runtime state store backed by Kafka Streams key-value state stores.
 *
 * <p>Kafka state store persists a portable snapshot, not a Java object
 * Detector runtime state store backed by Kafka Streams key-value state stores.
 * and adding schema migrations without changing the core engine.</p>
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


