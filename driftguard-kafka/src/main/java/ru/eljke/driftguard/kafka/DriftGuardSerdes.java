package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

/**
 * Готовые Kafka SerDes для публичных DriftGuard-сообщений.
 */
public final class DriftGuardSerdes {
    private DriftGuardSerdes() {
    }

    public static Serde<MetricPoint> metricPoint(ObjectMapper objectMapper) {
        return new JsonSerde<>(objectMapper, MetricPoint.class);
    }

    public static Serde<DriftEvent> driftEvent(ObjectMapper objectMapper) {
        return new JsonSerde<>(objectMapper, DriftEvent.class);
    }
}
