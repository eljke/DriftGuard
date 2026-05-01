package ru.eljke.driftguard.demo;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.DetectionMetrics;

import java.util.List;

/**
 * Snapshot результата последнего demo-прогона.
 */
public record DemoRunResult(
        String scenario,
        int metricPoints,
        List<MetricPoint> samplePoints,
        List<DriftEvent> events,
        DetectionMetrics quality
) {
}
