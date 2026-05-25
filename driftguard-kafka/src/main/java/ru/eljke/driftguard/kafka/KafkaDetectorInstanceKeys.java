package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Builds a stable Kafka state-store key for a concrete detector instance state.
 *
 * <p>The key includes the metric key and detector definition name because one metric can
 * be processed by several detectors with independent states.</p>
 */
public final class KafkaDetectorInstanceKeys {
    private static final String SEPARATOR = "|";
    private static final String NULL_VALUE = "~";

    private KafkaDetectorInstanceKeys() {
    }

    public static String stateKey(DetectorInstanceKey key) {
        DetectorInstanceKey nonNullKey = DriftGuardErrors.requireNonNull(key, "key");
        MetricKey metricKey = nonNullKey.metricKey();
        return String.join(
                SEPARATOR,
                encode(metricKey.service()),
                encode(metricKey.metric()),
                encode(metricKey.instance()),
                encode(metricKey.operation()),
                encode(nonNullKey.detectorName())
        );
    }

    private static String encode(String value) {
        if (value == null) {
            return NULL_VALUE;
        }
        return value.replace("%", "%25").replace(SEPARATOR, "%7C");
    }
}


