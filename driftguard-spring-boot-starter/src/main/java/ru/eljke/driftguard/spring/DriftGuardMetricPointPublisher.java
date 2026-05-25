package ru.eljke.driftguard.spring;

import lombok.RequiredArgsConstructor;
import ru.eljke.driftguard.core.adapter.MetricPointPublisher;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Spring adapter that exposes the configured detection engine through the
 * application-facing {@link MetricPointPublisher} port.
 */
@RequiredArgsConstructor
public final class DriftGuardMetricPointPublisher implements MetricPointPublisher {
    private final DriftDetectorEngine engine;

    @Override
    public List<DriftEvent> publish(MetricPoint point) {
        return engine.detect(point);
    }
}
