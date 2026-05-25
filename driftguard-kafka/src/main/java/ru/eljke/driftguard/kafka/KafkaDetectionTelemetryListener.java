package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Listener for Kafka Streams detection telemetry callbacks.
 *
 * Listener for Kafka Streams detection telemetry callbacks.
 * Listener for Kafka Streams detection telemetry callbacks.
 * Listener for Kafka Streams detection telemetry callbacks.
 */
public interface KafkaDetectionTelemetryListener {
    /**
     * Listener for Kafka Streams detection telemetry callbacks.
     *
     * @param point metric point being processed
     * @param events emitted drift events
     * @param durationNanos processing duration in nanoseconds
     */
    default void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
    }

    /**
     * Listener for Kafka Streams detection telemetry callbacks.
     *
     * @param point metric point being processed
     * @param exception processing exception
     * @param durationNanos processing duration before the failure, in nanoseconds
     */
    default void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
    }

    /**
     * Listener for Kafka Streams detection telemetry callbacks.
     *
     * @param error routed detection error
     */
    default void onDetectionErrorRouted(KafkaDetectionError error) {
    }

    static KafkaDetectionTelemetryListener noop() {
        return new KafkaDetectionTelemetryListener() {
        };
    }
}


