package ru.eljke.driftguard.testkit;

import java.util.List;
import java.util.Objects;

/**
 * Quality-gate evaluation result for a benchmark report.
 *
 * @param benchmarkReport benchmark report being checked
 * @param gate quality gate thresholds
 * @param violations quality-gate violations
 */
public record DetectionQualityReport(
        DetectionBenchmarkReport benchmarkReport,
        DetectionQualityGate gate,
        List<DetectionQualityViolation> violations
) {
    public DetectionQualityReport {
        benchmarkReport = Objects.requireNonNull(benchmarkReport, "benchmarkReport must not be null");
        gate = Objects.requireNonNull(gate, "gate must not be null");
        violations = List.copyOf(violations == null ? List.of() : violations);
    }

    public boolean passed() {
        return violations.isEmpty();
    }

    public String describe() {
        if (passed()) {
            return "Detection quality gate passed for benchmark '" + benchmarkReport.label() + "'";
        }
        return "Detection quality gate failed for benchmark '" + benchmarkReport.label() + "':\n"
                + String.join("\n", violations.stream().map(DetectionQualityViolation::describe).toList());
    }
}


