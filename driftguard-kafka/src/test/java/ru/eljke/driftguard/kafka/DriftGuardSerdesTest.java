package ru.eljke.driftguard.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftEventPhase;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.DriftGuardException;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DriftGuardSerdesTest {

    @Test
    void metricPointSerdeRoundTripsPublicMessageContract() {
        Serde<MetricPoint> serde = DriftGuardSerdes.metricPoint(DriftGuardObjectMapper.create());
        MetricPoint point = new MetricPoint(
                new MetricKey("checkout", "latency", "pod-1", "POST /orders"),
                Instant.parse("2026-05-01T10:15:30Z"),
                123.45,
                MetricKind.GAUGE,
                Map.of("region", "eu-west-1"),
                Map.of("source", "test")
        );

        MetricPoint decoded = roundTrip(serde, "metrics", point);

        assertEquals(point.key(), decoded.key());
        assertEquals(point.timestamp(), decoded.timestamp());
        assertEquals(point.value(), decoded.value());
        assertEquals(point.kind(), decoded.kind());
        assertEquals(point.tags(), decoded.tags());
        assertEquals(point.attributes(), decoded.attributes());
    }

    @Test
    void driftEventSerdeRoundTripsPublicMessageContract() {
        Serde<DriftEvent> serde = DriftGuardSerdes.driftEvent(DriftGuardObjectMapper.create());
        DriftEvent event = new DriftEvent(
                "event-1",
                new MetricKey("checkout", "latency", "pod-1", "POST /orders"),
                Instant.parse("2026-05-01T10:15:30Z"),
                Instant.parse("2026-05-01T10:14:30Z"),
                Instant.parse("2026-05-01T10:15:30Z"),
                DriftEventPhase.STARTED,
                DriftDirection.UP,
                DriftSeverity.CRITICAL,
                42.5,
                180.0,
                100.0,
                "latency-page-hinkley",
                "page-hinkley",
                "Mean shifted upward",
                Map.of("region", "eu-west-1"),
                Map.of("threshold", 25.0, "windowSize", 50)
        );

        DriftEvent decoded = roundTrip(serde, "drift-events", event);

        assertEquals(event.id(), decoded.id());
        assertEquals(event.key(), decoded.key());
        assertEquals(event.detectedAt(), decoded.detectedAt());
        assertEquals(event.windowStart(), decoded.windowStart());
        assertEquals(event.windowEnd(), decoded.windowEnd());
        assertEquals(event.phase(), decoded.phase());
        assertEquals(event.direction(), decoded.direction());
        assertEquals(event.severity(), decoded.severity());
        assertEquals(event.score(), decoded.score());
        assertEquals(event.currentValue(), decoded.currentValue());
        assertEquals(event.baselineValue(), decoded.baselineValue());
        assertEquals(event.detector(), decoded.detector());
        assertEquals(event.algorithm(), decoded.algorithm());
        assertEquals(event.reason(), decoded.reason());
        assertEquals(event.tags(), decoded.tags());
        assertEquals(25.0, decoded.details().get("threshold"));
        assertEquals(50, decoded.details().get("windowSize"));
    }

    @Test
    void serializersPreserveKafkaTombstones() {
        Serde<MetricPoint> serde = DriftGuardSerdes.metricPoint(DriftGuardObjectMapper.create());

        assertNull(serde.serializer().serialize("metrics", null));
        assertNull(serde.deserializer().deserialize("metrics", null));
        assertNull(serde.deserializer().deserialize("metrics", new byte[0]));
    }

    @Test
    void deserializerFailsFastOnInvalidJson() {
        Serde<MetricPoint> serde = DriftGuardSerdes.metricPoint(DriftGuardObjectMapper.create());

        assertThrows(
                DriftGuardException.class,
                () -> serde.deserializer().deserialize("metrics", "not-json".getBytes())
        );
    }

    private static <T> T roundTrip(Serde<T> serde, String topic, T value) {
        Serializer<T> serializer = serde.serializer();
        Deserializer<T> deserializer = serde.deserializer();
        return deserializer.deserialize(topic, serializer.serialize(topic, value));
    }
}
