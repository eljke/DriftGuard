package ru.eljke.driftguard.core.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Публичное событие, которое создаётся, когда detector делает вывод о дрейфе
 * потока метрик.
 *
 * <p>Событие содержит компактную общую схему и расширяемые map-поля. Общие
 * поля подходят для Kafka topic-ов, REST API, логов и UI-таблиц.
 * Алгоритм-специфичные доказательства следует помещать в {@link #details()}.</p>
 *
 * @param id уникальный id события; генерируется автоматически, если не задан
 * @param key поток метрик, в котором обнаружен drift
 * @param detectedAt время генерации события detector-ом
 * @param windowStart начало анализируемого окна, если известно
 * @param windowEnd конец анализируемого окна, если известно
 * @param direction направление или тип drift-а
 * @param severity уровень важности, вычисленный по настроенным порогам
 * @param score алгоритм-специфичная оценка drift-а
 * @param currentValue репрезентативное текущее значение
 * @param baselineValue репрезентативное baseline-значение
 * @param detector имя настроенного экземпляра detector-а
 * @param algorithm имя реализации алгоритма
 * @param reason короткое человекочитаемое объяснение
 * @param tags скопированные или обогащённые теги метрики
 * @param details алгоритм-специфичные детали и доказательства
 */
public record DriftEvent(
        String id,
        MetricKey key,
        Instant detectedAt,
        Instant windowStart,
        Instant windowEnd,
        DriftDirection direction,
        DriftSeverity severity,
        double score,
        double currentValue,
        double baselineValue,
        String detector,
        String algorithm,
        String reason,
        Map<String, String> tags,
        Map<String, Object> details
) {
    public DriftEvent {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        key = Objects.requireNonNull(key, "key must not be null");
        detectedAt = Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        windowStart = windowStart == null ? detectedAt : windowStart;
        windowEnd = windowEnd == null ? detectedAt : windowEnd;
        direction = direction == null ? DriftDirection.UNKNOWN : direction;
        severity = severity == null ? DriftSeverity.WARNING : severity;
        detector = normalize(detector, "detector");
        algorithm = normalize(algorithm, "algorithm");
        reason = reason == null ? "" : reason;
        tags = Map.copyOf(tags == null ? Map.of() : tags);
        details = Map.copyOf(details == null ? Map.of() : details);
    }

    private static String normalize(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
