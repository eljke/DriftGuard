package ru.eljke.driftguard.core.domain;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;
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
 * @param phase фаза жизненного цикла drift episode
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
        DriftEventPhase phase,
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
    public DriftEvent(
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
        this(id, key, detectedAt, windowStart, windowEnd, DriftEventPhase.STARTED, direction, severity, score,
                currentValue, baselineValue, detector, algorithm, reason, tags, details);
    }

    public DriftEvent {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        key = DriftGuardErrors.requireNonNull(key, "key");
        detectedAt = DriftGuardErrors.requireNonNull(detectedAt, "detectedAt");
        windowStart = windowStart == null ? detectedAt : windowStart;
        windowEnd = windowEnd == null ? detectedAt : windowEnd;
        phase = phase == null ? DriftEventPhase.STARTED : phase;
        direction = direction == null ? DriftDirection.UNKNOWN : direction;
        severity = severity == null ? DriftSeverity.WARNING : severity;
        detector = normalize(detector, "detector");
        algorithm = normalize(algorithm, "algorithm");
        reason = reason == null ? "" : reason;
        tags = Map.copyOf(tags == null ? Map.of() : tags);
        details = Map.copyOf(details == null ? Map.of() : details);
    }

    public DriftEvent recoveredAt(Instant recoveredAt, int recoveryConsecutiveNormal) {
        Map<String, Object> recoveryDetails = new LinkedHashMap<>(details);
        recoveryDetails.put("recoveryConsecutiveNormal", recoveryConsecutiveNormal);
        recoveryDetails.put("episodeRecovered", true);
        return new DriftEvent(
                null,
                key,
                recoveredAt,
                windowStart,
                recoveredAt,
                DriftEventPhase.RECOVERED,
                direction,
                DriftSeverity.INFO,
                0.0,
                currentValue,
                baselineValue,
                detector,
                algorithm,
                "Drift episode recovered after consecutive normal observations",
                tags,
                recoveryDetails
        );
    }

    private static String normalize(String value, String field) {
        return DriftGuardErrors.requireNonBlank(value, field);
    }
}
