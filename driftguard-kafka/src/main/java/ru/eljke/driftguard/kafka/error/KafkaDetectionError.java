package ru.eljke.driftguard.kafka.error;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;

/**
 * Serializable error record produced when Kafka detection fails.
 *
 * Serializable error record produced when Kafka detection fails.
 * Serializable error record produced when Kafka detection fails.
 * Serializable error record produced when Kafka detection fails.
 *
 * @param point metric point being processed
 * @param exceptionClass fully qualified exception class name
 * @param message error message
 * @param occurredAt time when the error occurred
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


