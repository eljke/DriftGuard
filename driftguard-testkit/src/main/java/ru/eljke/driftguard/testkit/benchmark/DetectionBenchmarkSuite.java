package ru.eljke.driftguard.testkit.benchmark;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.quality.DetectionQualityGate;
import ru.eljke.driftguard.testkit.quality.DetectionQualityReport;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Named group of reproducible detection scenarios.
 *
 * <p>A suite runs one detector function against every configured scenario and
 * returns aggregate precision, recall, false-positive and delay metrics. It is
 * intended for regression tests, release gates and thesis evaluation reports.</p>
 */
public record DetectionBenchmarkSuite(
        String label,
        List<DetectionBenchmarkCase> cases
) {
    public DetectionBenchmarkSuite {
        label = label == null || label.isBlank() ? "default" : label;
        cases = List.copyOf(cases == null ? List.of() : cases);
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("cases must not be empty");
        }
    }

    public static DetectionBenchmarkSuite of(String label, List<DetectionBenchmarkCase> cases) {
        return new DetectionBenchmarkSuite(label, cases);
    }

    public DetectionBenchmarkReport run(Function<MetricPoint, List<DriftEvent>> detector) {
        Objects.requireNonNull(detector, "detector must not be null");
        List<DetectionBenchmarkResult> results = cases.stream()
                .map(testCase -> DetectionBenchmarkRunner.runScenario(testCase.id(), testCase.scenario(), detector))
                .toList();
        return DetectionBenchmarkRunner.report(label, results);
    }

    public DetectionQualityReport assertQuality(
            Function<MetricPoint, List<DriftEvent>> detector,
            DetectionQualityGate gate
    ) {
        Objects.requireNonNull(gate, "gate must not be null");
        DetectionBenchmarkReport report = run(detector);
        gate.assertPassed(report);
        return gate.evaluate(report);
    }
}


