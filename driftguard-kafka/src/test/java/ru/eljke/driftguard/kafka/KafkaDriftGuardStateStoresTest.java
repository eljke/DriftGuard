package ru.eljke.driftguard.kafka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaDriftGuardStateStoresTest {

    @Test
    void createsRuntimeStateStoreWithConfiguredName() {
        var storeBuilder = KafkaDriftGuardStateStores.runtimeStateStore(
                "custom-runtime-state",
                DriftGuardObjectMapper.create()
        );

        assertEquals("custom-runtime-state", storeBuilder.name());
    }
}
