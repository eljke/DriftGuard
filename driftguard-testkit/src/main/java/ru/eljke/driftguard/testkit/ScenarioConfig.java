package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Common configuration shared by synthetic metric scenarios.
 *
 * @param key metric stream key
 * @param kind metric kind
 * @param start start timestamp
 * @param step time step between generated samples
 * @param samples number of generated samples
 * @param seed random seed for reproducible noise
 */
public record ScenarioConfig(
        MetricKey key,
        MetricKind kind,
        Instant start,
        Duration step,
        int samples,
        long seed
) {
    public ScenarioConfig {
        key = Objects.requireNonNull(key, "key must not be null");
        kind = Objects.requireNonNull(kind, "kind must not be null");
        start = Objects.requireNonNull(start, "start must not be null");
        step = Objects.requireNonNull(step, "step must not be null");
        if (step.isZero() || step.isNegative()) {
            throw new IllegalArgumentException("step must be positive");
        }
        if (samples <= 0) {
            throw new IllegalArgumentException("samples must be positive");
        }
    }

    public static ScenarioConfig latency(String service, String operation, int samples) {
        return metric(service, "latency", operation, MetricKind.DURATION, samples);
    }

    public static ScenarioConfig errorRate(String service, String operation, int samples) {
        return metric(service, "error-rate", operation, MetricKind.RATE, samples);
    }

    public static ScenarioConfig throughput(String service, String operation, int samples) {
        return metric(service, "throughput", operation, MetricKind.RATE, samples);
    }

    public static ScenarioConfig queueSize(String service, String operation, int samples) {
        return metric(service, "queue-size", operation, MetricKind.SIZE, samples);
    }

    public static ScenarioConfig metric(String service, String metric, String operation, MetricKind kind, int samples) {
        return new ScenarioConfig(
                new MetricKey(service, metric, "instance-1", operation),
                kind,
                Instant.parse("2026-05-01T10:00:00Z"),
                Duration.ofSeconds(1),
                samples,
                42L
        );
    }
}


