package ru.eljke.driftguard.spring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ru.eljke.driftguard.core.detector.DriftDetectionListener;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer listener для базовых метрик detection pipeline.
 */
public final class MicrometerDriftDetectionListener implements DriftDetectionListener {
    private final MeterRegistry registry;

    public MicrometerDriftDetectionListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        MetricKey key = point.key();
        Counter.builder("driftguard.detection.points")
                .description("Количество обработанных MetricPoint")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .register(registry)
                .increment();

        Timer.builder("driftguard.detection.duration")
                .description("Длительность обработки одной MetricPoint")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        for (DriftEvent event : events) {
            Counter.builder("driftguard.detection.events")
                    .description("Количество опубликованных DriftEvent")
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
        Counter.builder("driftguard.detection.errors")
                .description("Количество ошибок detection pipeline")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("exception", tag(exception.getClass().getSimpleName()))
                .register(registry)
                .increment();

        Timer.builder("driftguard.detection.duration")
                .description("Длительность обработки одной MetricPoint")
                .tag("service", tag(key.service()))
                .tag("metric", tag(key.metric()))
                .tag("outcome", "error")
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private static String tag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
