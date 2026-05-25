package ru.eljke.driftguard.core.domain;

import lombok.Builder;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;

/**
 * Стабильная идентичность потока метрик, который обрабатывает DriftGuard.
 *
 * <p>Ключ намеренно небольшой и не зависит от транспорта. Наиболее частые
 * измерения вынесены в отдельные поля, а менее универсальные измерения следует
 * передавать через {@link MetricPoint#tags()}.</p>
 *
 * @param service сервис или подсистема, которая сгенерировала метрику
 * @param metric имя метрики, например {@code latency}, {@code error_rate} или {@code cpu_usage}
 * @param instance необязательный id экземпляра сервиса, pod-а, node-а или host-а
 * @param operation необязательное имя операции, endpoint-а, job-а или consumer group
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
