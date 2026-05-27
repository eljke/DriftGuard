package ru.eljke.driftguard.kafka.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import ru.eljke.driftguard.core.error.DriftGuardErrors;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateSnapshot;
import ru.eljke.driftguard.kafka.serde.DriftGuardSerdes;

/**
 * Factory for Kafka Streams state stores used by DriftGuard.
 */
public final class KafkaDriftGuardStateStores {
    public static final String DEFAULT_RUNTIME_STATE_STORE = "driftguard-runtime-state";

    private KafkaDriftGuardStateStores() {
    }

    public static StoreBuilder<KeyValueStore<String, DetectorRuntimeStateSnapshot>> runtimeStateStore(
            ObjectMapper objectMapper
    ) {
        return runtimeStateStore(DEFAULT_RUNTIME_STATE_STORE, objectMapper);
    }

    public static StoreBuilder<KeyValueStore<String, DetectorRuntimeStateSnapshot>> runtimeStateStore(
            String storeName,
            ObjectMapper objectMapper
    ) {
        String nonBlankStoreName = DriftGuardErrors.requireNonBlank(storeName, "storeName");
        ObjectMapper nonNullObjectMapper = DriftGuardErrors.requireNonNull(objectMapper, "objectMapper");
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(nonBlankStoreName),
                Serdes.String(),
                DriftGuardSerdes.runtimeStateSnapshot(nonNullObjectMapper)
        );
    }
}


