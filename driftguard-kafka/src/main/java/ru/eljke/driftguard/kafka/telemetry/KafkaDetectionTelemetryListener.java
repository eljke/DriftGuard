package ru.eljke.driftguard.kafka.telemetry;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.kafka.error.KafkaDetectionError;

import java.util.List;

/**
 * Listener for Kafka Streams detection telemetry callbacks.
 */
public interface KafkaDetectionTelemetryListener {
    /**
     * Called after a metric point has been processed successfully.
     *
     * @param point metric point being processed
     * @param events emitted drift events
     * @param durationNanos processing duration in nanoseconds
     */
    default void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
    }

    /**
     * Called when metric-point processing fails.
     *
     * @param point metric point being processed
     * @param exception processing exception
     * @param durationNanos processing duration before the failure, in nanoseconds
     */
    default void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
    }

    /**
     * Called when an error has been routed to the configured error topic.
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


