package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * English API documentation.
 *
 * English API documentation.
 * English API documentation.
 * English API documentation.
 */
public interface KafkaDetectionTelemetryListener {
    /**
     * English API documentation.
     *
     * @param point documented value
     * @param events documented value
     * @param durationNanos processing duration in nanoseconds
     */
    default void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
    }

    /**
     * English API documentation.
     *
     * @param point documented value
     * @param exception documented value
     * @param durationNanos processing duration before the failure, in nanoseconds
     */
    default void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
    }

    /**
     * English API documentation.
     *
     * @param error documented value
     */
    default void onDetectionErrorRouted(KafkaDetectionError error) {
    }

    static KafkaDetectionTelemetryListener noop() {
        return new KafkaDetectionTelemetryListener() {
        };
    }
}


