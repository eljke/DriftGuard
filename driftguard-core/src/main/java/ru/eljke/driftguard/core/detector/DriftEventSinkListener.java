package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.sink.DriftEventSink;

import java.util.List;

/**
 * Listener that delivers published {@link DriftEvent} to a set of sinks.
 *
 * <p>Individual sink errors do not interrupt the detection pipeline: the event is already
 * considered published by the engine, and a sink is an external adapter layer.</p>
 */
public final class DriftEventSinkListener implements DriftDetectionListener {
    private final List<DriftEventSink> sinks;

    public DriftEventSinkListener(List<DriftEventSink> sinks) {
        this.sinks = List.copyOf(sinks == null ? List.of() : sinks);
    }

    @Override
    public void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        if (events == null || events.isEmpty() || sinks.isEmpty()) {
            return;
        }
        for (DriftEvent event : events) {
            publish(event);
        }
    }

    private void publish(DriftEvent event) {
        for (DriftEventSink sink : sinks) {
            try {
                sink.accept(event);
            } catch (RuntimeException ignored) {
                // Sink errors must not break the main detection pipeline.
            }
        }
    }
}

