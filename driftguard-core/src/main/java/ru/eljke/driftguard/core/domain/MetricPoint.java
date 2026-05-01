package ru.eljke.driftguard.core.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Одно наблюдаемое значение в потоке технических метрик.
 *
 * <p>Это основной входной контракт core-модуля. Тип намеренно не содержит
 * Kafka-, Spring-, JSON- или Prometheus-специфичных полей. Adapter-ы должны
 * преобразовать свой нативный формат события в этот record перед вызовом
 * detection engine.</p>
 *
 * @param key стабильная идентичность метрики для маршрутизации и поиска состояния
 * @param timestamp event timestamp наблюдаемого значения
 * @param value числовое значение метрики; должно быть конечным
 * @param kind семантический тип метрики
 * @param tags индексируемые строковые измерения для фильтрации и вывода событий
 * @param attributes дополнительная неиндексируемая нагрузка для adapter-ов и диагностики
 */
public record MetricPoint(
        MetricKey key,
        Instant timestamp,
        double value,
        MetricKind kind,
        Map<String, String> tags,
        Map<String, Object> attributes
) {
    public MetricPoint {
        key = Objects.requireNonNull(key, "key must not be null");
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        kind = Objects.requireNonNull(kind, "kind must not be null");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
        tags = Map.copyOf(tags == null ? Map.of() : tags);
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
    }

    public static MetricPoint gauge(MetricKey key, Instant timestamp, double value) {
        return new MetricPoint(key, timestamp, value, MetricKind.GAUGE, Map.of(), Map.of());
    }
}
