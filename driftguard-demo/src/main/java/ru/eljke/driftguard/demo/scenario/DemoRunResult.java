package ru.eljke.driftguard.demo.scenario;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.DetectionMetrics;
import ru.eljke.driftguard.testkit.DriftInterval;

import java.util.List;

/**
 * Snapshot of the latest demo run result.
 */
public record DemoRunResult(
        String scenario,
        String title,
        String mode,
        boolean running,
        int processedPoints,
        int metricPoints,
        List<MetricPoint> samplePoints,
        List<DriftInterval> expectedDrifts,
        List<DriftEvent> events,
        DetectionMetrics quality
) {
}


