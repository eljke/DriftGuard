package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;

/**
 * Сообщение об ошибке обработки метрики внутри Kafka Streams pipeline.
 *
 * <p>Объект предназначен для отправки в отдельный error topic. Он хранит исходную
 * метрику и минимальную диагностическую информацию об исключении, не привязывая
 * публичный контракт к конкретному logging framework-у.</p>
 *
 * @param point исходная метрика, на которой упала обработка
 * @param exceptionClass полное имя класса исключения
 * @param message сообщение исключения
 * @param occurredAt время фиксации ошибки
 */
public record KafkaDetectionError(
        MetricPoint point,
        String exceptionClass,
        String message,
        Instant occurredAt
) {
    public static KafkaDetectionError from(MetricPoint point, RuntimeException exception, Instant occurredAt) {
        return new KafkaDetectionError(
                point,
                exception.getClass().getName(),
                exception.getMessage(),
                occurredAt
        );
    }
}
