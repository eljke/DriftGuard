package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Запускает synthetic scenarios через переданную функцию детекции и считает
 * качество detector-а на воспроизводимых потоках метрик.
 */
public final class DetectionBenchmarkRunner {
    private DetectionBenchmarkRunner() {
    }

    public static DetectionBenchmarkResult runScenario(
            String scenarioId,
            MetricScenario scenario,
            Function<MetricPoint, List<DriftEvent>> detector
    ) {
        List<DriftEvent> events = new ArrayList<>();
        for (MetricPoint point : scenario.generate()) {
            events.addAll(detector.apply(point));
        }
        return new DetectionBenchmarkResult(scenarioId, DetectionEvaluator.evaluate(scenario, events));
    }

    public static DetectionBenchmarkReport report(String label, List<DetectionBenchmarkResult> results) {
        return new DetectionBenchmarkReport(label, results, DetectionBenchmarkSummary.from(results));
    }

    public static DetectionQualityReport assertQuality(
            String label,
            List<DetectionBenchmarkResult> results,
            DetectionQualityGate gate
    ) {
        DetectionBenchmarkReport report = report(label, results);
        gate.assertPassed(report);
        return gate.evaluate(report);
    }
}
