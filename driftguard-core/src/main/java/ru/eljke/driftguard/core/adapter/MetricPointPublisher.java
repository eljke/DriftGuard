package ru.eljke.driftguard.core.adapter;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Application-facing port for publishing observations into DriftGuard.
 *
 * <p>Infrastructure adapters should convert their native input, such as HTTP
 * request timings, Micrometer samples or Kafka messages, to {@link MetricPoint}
 * and send them through this port. The return value contains public drift
 * events emitted for the point.</p>
 */
@FunctionalInterface
public interface MetricPointPublisher {
    /**
     * Publishes one metric point to the configured detection runtime.
     *
     * @param point observed metric value
     * @return immutable list of emitted drift events
     */
    List<DriftEvent> publish(MetricPoint point);
}
