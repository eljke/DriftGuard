package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Общие настройки синтетического сценария метрик.
 *
 * @param key ключ генерируемой метрики
 * @param kind тип метрики
 * @param start начало сценария
 * @param step шаг между точками
 * @param samples число генерируемых точек
 * @param seed seed генератора шума
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
        return new ScenarioConfig(
                new MetricKey(service, "latency", "instance-1", operation),
                MetricKind.DURATION,
                Instant.parse("2026-05-01T10:00:00Z"),
                Duration.ofSeconds(1),
                samples,
                42L
        );
    }
}
