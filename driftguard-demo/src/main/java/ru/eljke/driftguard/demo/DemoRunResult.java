package ru.eljke.driftguard.demo;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.DetectionMetrics;
import ru.eljke.driftguard.testkit.DriftInterval;

import java.util.List;

/**
 * Snapshot результата последнего demo-прогона.
 */
public record DemoRunResult(
        String scenario,
        String title,
        int metricPoints,
        List<MetricPoint> samplePoints,
        List<DriftInterval> expectedDrifts,
        List<DriftEvent> events,
        DetectionMetrics quality
) {
}
