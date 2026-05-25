package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;

/**
 * English API documentation.
 *
 * English API documentation.
 * English API documentation.
 * English API documentation.
 *
 * @param point documented value
 * @param exceptionClass documented value
 * @param message documented value
 * @param occurredAt documented value
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


