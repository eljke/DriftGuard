package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Strategy invoked when detector execution fails inside Kafka Streams processing.
 *
 * <p>The default policy is fail-fast. For operational pipelines that prefer keeping the stream alive,
 * use {@link #skip()} and add external monitoring around processing failures.</p>
 */
@FunctionalInterface
public interface KafkaDetectionErrorHandler {
    List<DriftEvent> handle(MetricPoint point, RuntimeException exception);

    static KafkaDetectionErrorHandler failFast() {
        return (point, exception) -> {
            throw exception;
        };
    }

    static KafkaDetectionErrorHandler skip() {
        return (point, exception) -> List.of();
    }
}
