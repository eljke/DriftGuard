package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.sink.DriftEventSink;

import java.util.List;

/**
 * Listener, который доставляет опубликованные {@link DriftEvent} в набор sink-ов.
 *
 * <p>Ошибки отдельных sink-ов не прерывают detection pipeline: событие уже
 * считается опубликованным engine-ом, а sink является внешним adapter-слоем.</p>
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
                // Sink-ошибка не должна ломать основной detection pipeline.
            }
        }
    }
}
