package ru.eljke.driftguard.kafka.state;
import ru.eljke.driftguard.kafka.error.*;
import ru.eljke.driftguard.kafka.serde.*;
import ru.eljke.driftguard.kafka.state.*;
import ru.eljke.driftguard.kafka.telemetry.*;
import ru.eljke.driftguard.kafka.topology.*;
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
