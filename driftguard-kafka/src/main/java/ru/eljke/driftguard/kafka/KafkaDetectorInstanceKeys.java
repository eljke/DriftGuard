package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Формирует стабильный ключ Kafka state store для состояния конкретного detector instance.
 *
 * <p>Ключ включает metric key и имя detector definition, потому что одна метрика может
 * обрабатываться несколькими detector-ами с независимыми состояниями.</p>
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
