package ru.eljke.driftguard.spring.metrics;
import ru.eljke.driftguard.spring.alert.*;
import ru.eljke.driftguard.spring.autoconfigure.*;
import ru.eljke.driftguard.spring.input.*;
import ru.eljke.driftguard.spring.kafka.*;
import ru.eljke.driftguard.spring.metrics.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftEventPhase;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.kafka.error.KafkaDetectionError;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerKafkaDetectionTelemetryListenerTest {
    private static final MetricKey KEY = MetricKey.of("orders", "latency");
    private static final MetricPoint POINT = MetricPoint.gauge(KEY, Instant.parse("2026-05-03T00:00:00Z"), 42.0);

    @Test
    void recordsSuccessfulKafkaDetectionMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerKafkaDetectionTelemetryListener listener = new MicrometerKafkaDetectionTelemetryListener(registry);

        listener.onDetectionCompleted(POINT, List.of(event()), 1_000_000L);

        assertEquals(1.0, registry.counter(
                "driftguard.kafka.detection.points",
                "service", "orders",
                "metric", "latency",
                "outcome", "success"
        ).count());
        assertEquals(1.0, registry.counter(
                "driftguard.kafka.detection.events",
                "service", "orders",
                "metric", "latency",
                "detector", "latency-page-hinkley",
                "algorithm", "page-hinkley",
                "severity", "WARNING",
                "phase", "STARTED"
        ).count());
        assertEquals(1, registry.timer(
                "driftguard.kafka.detection.duration",
                "service", "orders",
                "metric", "latency",
                "outcome", "success"
        ).count());
    }

    @Test
    void recordsKafkaDetectionFailureAndRoutedError() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerKafkaDetectionTelemetryListener listener = new MicrometerKafkaDetectionTelemetryListener(registry);

        RuntimeException exception = new IllegalStateException("boom");
        listener.onDetectionFailed(POINT, exception, 2_000_000L);
        listener.onDetectionErrorRouted(KafkaDetectionError.from(POINT, exception, POINT.timestamp()));

        assertEquals(1.0, registry.counter(
                "driftguard.kafka.detection.errors",
                "service", "orders",
                "metric", "latency",
                "exception", "IllegalStateException"
        ).count());
        assertEquals(1.0, registry.counter(
                "driftguard.kafka.detection.errors.routed",
                "service", "orders",
                "metric", "latency",
                "exception", "IllegalStateException"
        ).count());
        assertEquals(1, registry.timer(
                "driftguard.kafka.detection.duration",
                "service", "orders",
                "metric", "latency",
                "outcome", "error"
        ).count());
    }

    private static DriftEvent event() {
        return new DriftEvent(
                "event-1",
                KEY,
                Instant.parse("2026-05-03T00:00:01Z"),
                Instant.parse("2026-05-03T00:00:00Z"),
                Instant.parse("2026-05-03T00:00:01Z"),
                DriftEventPhase.STARTED,
                DriftDirection.UP,
                DriftSeverity.WARNING,
                1.5,
                42.0,
                20.0,
                "latency-page-hinkley",
                "page-hinkley",
                "Latency drift",
                Map.of(),
                Map.of()
        );
    }
}
