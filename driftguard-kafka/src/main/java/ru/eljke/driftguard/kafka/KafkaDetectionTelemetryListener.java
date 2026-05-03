package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Listener для технических событий Kafka detection pipeline.
 *
 * <p>Контракт намеренно лёгкий: Kafka adapter не зависит от Micrometer,
 * logging framework-ов или Spring. Интеграционный слой может повесить сюда
 * собственные counters/timers/tracing, а core-логика останется независимой.</p>
 */
public interface KafkaDetectionTelemetryListener {
    /**
     * Вызывается после успешной обработки метрики detector-ом.
     *
     * @param point исходная метрика
     * @param events события drift-а, созданные по этой метрике
     * @param durationNanos длительность обработки в наносекундах
     */
    default void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
    }

    /**
     * Вызывается, если detector бросил исключение.
     *
     * @param point исходная метрика
     * @param exception исключение detector-а
     * @param durationNanos длительность обработки до ошибки в наносекундах
     */
    default void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
    }

    /**
     * Вызывается перед отправкой diagnostic payload-а в error topic.
     *
     * @param error диагностическое сообщение
     */
    default void onDetectionErrorRouted(KafkaDetectionError error) {
    }

    static KafkaDetectionTelemetryListener noop() {
        return new KafkaDetectionTelemetryListener() {
        };
    }
}
