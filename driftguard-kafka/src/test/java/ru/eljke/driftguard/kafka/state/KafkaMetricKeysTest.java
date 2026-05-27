package ru.eljke.driftguard.kafka.state;
import ru.eljke.driftguard.kafka.error.*;
import ru.eljke.driftguard.kafka.serde.*;
import ru.eljke.driftguard.kafka.state.*;
import ru.eljke.driftguard.kafka.telemetry.*;
import ru.eljke.driftguard.kafka.topology.*;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.MetricKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaMetricKeysTest {
    @Test
    void buildsStableStateKeyFromMetricIdentity() {
        MetricKey key = new MetricKey("checkout", "latency", "pod-1", "POST /orders");

        assertEquals("checkout|latency|pod-1|POST /orders", KafkaMetricKeys.stateKey(key));
    }

    @Test
    void usesNullMarkerForOptionalIdentityParts() {
        MetricKey key = MetricKey.of("checkout", "latency");

        assertEquals("checkout|latency|-|-", KafkaMetricKeys.stateKey(key));
    }

    @Test
    void separatesDifferentMetricIdentities() {
        String first = KafkaMetricKeys.stateKey(new MetricKey("checkout", "latency", "pod-1", null));
        String second = KafkaMetricKeys.stateKey(new MetricKey("checkout", "latency", "pod-2", null));

        assertNotEquals(first, second);
    }

    @Test
    void rejectsNullMetricKey() {
        assertThrows(NullPointerException.class, () -> KafkaMetricKeys.stateKey(null));
    }
}
