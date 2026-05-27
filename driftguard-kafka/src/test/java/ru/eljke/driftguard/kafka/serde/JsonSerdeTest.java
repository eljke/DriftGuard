package ru.eljke.driftguard.kafka.serde;
import ru.eljke.driftguard.kafka.error.*;
import ru.eljke.driftguard.kafka.serde.*;
import ru.eljke.driftguard.kafka.state.*;
import ru.eljke.driftguard.kafka.telemetry.*;
import ru.eljke.driftguard.kafka.topology.*;
import org.junit.jupiter.api.Test;
import org.apache.kafka.common.serialization.Serde;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonSerdeTest {
    @Test
    void serializesAndDeserializesMetricPoint() {
        Serde<MetricPoint> serde = DriftGuardSerdes.metricPoint(DriftGuardObjectMapper.create());
        MetricPoint point = MetricPoint.gauge(MetricKey.of("orders", "latency"), Instant.parse("2026-05-01T10:00:00Z"), 123.0);

        byte[] bytes = serde.serializer().serialize("metrics", point);
        MetricPoint restored = serde.deserializer().deserialize("metrics", bytes);

        assertEquals(point.key(), restored.key());
        assertEquals(point.timestamp(), restored.timestamp());
        assertEquals(point.value(), restored.value());
    }
}
