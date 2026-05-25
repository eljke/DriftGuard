package ru.eljke.driftguard.core.alert;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.detector.DriftDetectionListener;

import java.util.List;

/**
 * Detection listener that maps emitted drift events to user-facing alerts and
 * delivers them to configured alert sinks.
 */
public final class DriftAlertListener implements DriftDetectionListener {
    private final DriftAlertMapper mapper;
    private final List<DriftAlertSink> sinks;

    public DriftAlertListener(DriftAlertMapper mapper, List<DriftAlertSink> sinks) {
        this.mapper = mapper == null ? new DefaultDriftAlertMapper() : mapper;
        this.sinks = List.copyOf(sinks == null ? List.of() : sinks);
    }

    @Override
    public void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        if (events == null || events.isEmpty() || sinks.isEmpty()) {
            return;
        }
        for (DriftEvent event : events) {
            publish(mapper.map(event));
        }
    }

    private void publish(DriftAlert alert) {
        for (DriftAlertSink sink : sinks) {
            try {
                sink.accept(alert);
            } catch (RuntimeException ignored) {
                // Alert sink errors must not break the main detection pipeline.
            }
        }
    }
}
