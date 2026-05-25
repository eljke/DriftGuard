package ru.eljke.driftguard.spring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import ru.eljke.driftguard.core.adapter.MetricPointPublisher;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer input adapter that periodically samples configured meters and
 * publishes them as {@link MetricPoint} observations.
 */
@RequiredArgsConstructor
public final class MicrometerMetricInputPoller implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricInputPoller.class);

    private final MeterRegistry meterRegistry;
    private final MetricPointPublisher publisher;
    private final DriftGuardProperties.MicrometerInputProperties properties;
    private ScheduledExecutorService executor;
    private volatile boolean running;

    @Override
    public void start() {
        if (running || properties.getMeters().isEmpty()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "driftguard-micrometer-input");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                this::safePoll,
                0,
                properties.getPollInterval().toMillis(),
                TimeUnit.MILLISECONDS
        );
        running = true;
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.isAutoStartup();
    }

    void pollOnce() {
        Instant observedAt = Instant.now();
        for (DriftGuardProperties.MicrometerMeterProperties meter : properties.getMeters()) {
            value(meter).ifPresent(value -> publisher.publish(point(meter, value, observedAt)));
        }
    }

    private void safePoll() {
        try {
            pollOnce();
        } catch (RuntimeException exception) {
            log.warn("DriftGuard Micrometer input polling failed", exception);
        }
    }

    private java.util.Optional<Double> value(DriftGuardProperties.MicrometerMeterProperties meter) {
        Search search = meterRegistry.find(meter.getName());
        for (Map.Entry<String, String> tag : meter.getTags().entrySet()) {
            search = search.tag(tag.getKey(), tag.getValue());
        }
        return switch (meter.getType()) {
            case GAUGE -> finite(search.gauge(), Gauge::value);
            case COUNTER -> finite(search.counter(), Counter::count);
            case TIMER_MEAN -> finite(search.timer(), timer -> timer.mean(TimeUnit.MILLISECONDS));
            case TIMER_MAX -> finite(search.timer(), timer -> timer.max(TimeUnit.MILLISECONDS));
        };
    }

    private MetricPoint point(
            DriftGuardProperties.MicrometerMeterProperties meter,
            double value,
            Instant observedAt
    ) {
        return MetricPoint.builder()
                .key(MetricKey.builder()
                        .service(meter.getService())
                        .metric(meter.getMetric())
                        .operation(meter.getOperation())
                        .instance(meter.getInstance())
                        .build())
                .timestamp(observedAt)
                .value(value)
                .kind(kind(meter))
                .tags(meter.getTags())
                .attributes(Map.of("micrometerMeter", meter.getName(), "micrometerType", meter.getType().name()))
                .build();
    }

    private MetricKind kind(DriftGuardProperties.MicrometerMeterProperties meter) {
        if (meter.getKind() != null) {
            return meter.getKind();
        }
        return switch (meter.getType()) {
            case COUNTER -> MetricKind.COUNTER;
            case TIMER_MEAN, TIMER_MAX -> MetricKind.DURATION;
            case GAUGE -> MetricKind.GAUGE;
        };
    }

    private static <T> java.util.Optional<Double> finite(T meter, MeterValue<T> value) {
        if (meter == null) {
            return java.util.Optional.empty();
        }
        double measured = value.read(meter);
        return Double.isFinite(measured) ? java.util.Optional.of(measured) : java.util.Optional.empty();
    }

    @FunctionalInterface
    private interface MeterValue<T> {
        double read(T meter);
    }
}
