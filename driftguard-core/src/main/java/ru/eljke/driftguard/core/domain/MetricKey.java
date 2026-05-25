package ru.eljke.driftguard.core.domain;

import lombok.Builder;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;

/**
 * Stable identity of a metric stream processed by DriftGuard.
 *
 * <p>The key is intentionally small and transport-independent. The most common
 * dimensions are separate fields; less universal dimensions should be
 * passed through {@link MetricPoint#tags()}.</p>
 *
 * @param service service or subsystem that generated the metric
 * @param metric metric name, for example {@code latency}, {@code error_rate} or {@code cpu_usage}
 * @param instance optional id of a service instance, pod, node or host
 * @param operation optional operation, endpoint, job or consumer group name
 */
@Builder(toBuilder = true)
public record MetricKey(
        String service,
        String metric,
        String instance,
        String operation
) {
    public MetricKey {
        service = normalizeRequired(service, "service");
        metric = normalizeRequired(metric, "metric");
        instance = normalizeOptional(instance);
        operation = normalizeOptional(operation);
    }

    public static MetricKey of(String service, String metric) {
        return new MetricKey(service, metric, null, null);
    }

    public Optional<String> instanceValue() {
        return Optional.ofNullable(instance);
    }

    public Optional<String> operationValue() {
        return Optional.ofNullable(operation);
    }

    private static String normalizeRequired(String value, String field) {
        return DriftGuardErrors.requireNonBlank(value, field);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}


