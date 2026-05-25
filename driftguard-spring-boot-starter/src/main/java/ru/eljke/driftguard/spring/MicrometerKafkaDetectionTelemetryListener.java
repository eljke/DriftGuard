package ru.eljke.driftguard.spring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.kafka.KafkaDetectionError;
import ru.eljke.driftguard.kafka.KafkaDetectionTelemetryListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Kafka detection telemetry listener that records Micrometer metrics.
 *
 * Kafka detection telemetry listener that records Micrometer metrics.
 * Kafka detection telemetry listener that records Micrometer metrics.
 * Kafka detection telemetry listener that records Micrometer metrics.
 */
public final class MicrometerKafkaDetectionTelemetryListener implements KafkaDetectionTelemetryListener {
    private final MeterRegistry registry;

    public MicrometerKafkaDetectionTelemetryListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        MetricKey key = point.key();
        Counter.builder("driftguard.kafka.detection.points")
                .description("Number of MetricPoint objects processed by the Kafka topology")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("outcome", "success")
                .register(registry)
                .increment();

        Timer.builder("driftguard.kafka.detection.duration")
                .description("MetricPoint processing duration inside Kafka topology")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("outcome", "success")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        for (DriftEvent event : safeEvents(events)) {
            Counter.builder("driftguard.kafka.detection.events")
                    .description("Number of DriftEvent objects created by Kafka topology")
                    .tag("service", tag(event.key().service()))
                    .tag("metric", tag(event.key().metric()))
                    .tag("detector", tag(event.detector()))
                    .tag("algorithm", tag(event.algorithm()))
                    .tag("severity", tag(event.severity().name()))
                    .tag("phase", tag(event.phase().name()))
                    .register(registry)
                    .increment();
        }
    }

    @Override
    public void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
        MetricKey key = point.key();
        Counter.builder("driftguard.kafka.detection.errors")
                .description("Number of detector errors inside Kafka topology")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("exception", tag(exception.getClass().getSimpleName()))
                .register(registry)
                .increment();

        Timer.builder("driftguard.kafka.detection.duration")
                .description("MetricPoint processing duration inside Kafka topology")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("outcome", "error")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void onDetectionErrorRouted(KafkaDetectionError error) {
        MetricKey key = error.point().key();
        Counter.builder("driftguard.kafka.detection.errors.routed")
                .description("Number of diagnostic messages sent to the Kafka error topic")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("exception", tag(simpleClassName(error.exceptionClass())))
                .register(registry)
                .increment();
    }

    private static List<DriftEvent> safeEvents(List<DriftEvent> events) {
        return events == null ? List.of() : events;
    }

    private static String simpleClassName(String className) {
        if (className == null || className.isBlank()) {
            return "unknown";
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot < 0 ? className : className.substring(lastDot + 1);
    }

    private static String tag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}


