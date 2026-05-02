package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.Objects;

/**
 * Builds stable Kafka keys for DriftGuard internal state routing.
 */
public final class KafkaMetricKeys {
    private static final String NULL_VALUE = "-";
    private static final String SEPARATOR = "|";

    private KafkaMetricKeys() {
    }

    public static String stateKey(MetricKey key) {
        Objects.requireNonNull(key, "key must not be null");

        return String.join(
                SEPARATOR,
                encode(key.service()),
                encode(key.metric()),
                encode(key.instance()),
                encode(key.operation())
        );
    }

    private static String encode(String value) {
        if (value == null) {
            return NULL_VALUE;
        }
        return value.replace("%", "%25").replace(SEPARATOR, "%7C");
    }

}