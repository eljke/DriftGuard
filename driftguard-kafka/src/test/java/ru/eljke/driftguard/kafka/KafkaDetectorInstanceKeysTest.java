package ru.eljke.driftguard.kafka;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaDetectorInstanceKeysTest {
    @Test
    void includesMetricKeyAndDetectorName() {
        DetectorInstanceKey key = new DetectorInstanceKey(
                new MetricKey("orders", "latency", "api-1", "checkout"),
                "latency-page-hinkley"
        );

        assertEquals("orders|latency|api-1|checkout|latency-page-hinkley", KafkaDetectorInstanceKeys.stateKey(key));
    }

    @Test
    void separatesStatesForDifferentDetectorDefinitions() {
        MetricKey metricKey = MetricKey.of("orders", "latency");

        String firstKey = KafkaDetectorInstanceKeys.stateKey(new DetectorInstanceKey(metricKey, "fast-detector"));
        String secondKey = KafkaDetectorInstanceKeys.stateKey(new DetectorInstanceKey(metricKey, "slow-detector"));

        assertNotEquals(firstKey, secondKey);
    }

    @Test
    void escapesSeparatorAndPercentCharacters() {
        DetectorInstanceKey key = new DetectorInstanceKey(
                MetricKey.of("orders|api", "latency%p95"),
                "detector|main%v1"
        );

        assertEquals("orders%7Capi|latency%25p95|~|~|detector%7Cmain%25v1", KafkaDetectorInstanceKeys.stateKey(key));
    }

    @Test
    void rejectsNullKey() {
        assertThrows(DriftGuardValidationException.class, () -> KafkaDetectorInstanceKeys.stateKey(null));
    }
}
