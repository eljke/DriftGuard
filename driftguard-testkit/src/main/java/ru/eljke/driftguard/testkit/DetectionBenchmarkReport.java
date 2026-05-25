package ru.eljke.driftguard.testkit;

import java.util.List;

/**
 * Benchmark report containing per-scenario results and aggregate summary.
 *
 * @param label benchmark report label
 * @param results per-scenario benchmark results
 * @param summary aggregate benchmark summary
 */
public record DetectionBenchmarkReport(
        String label,
        List<DetectionBenchmarkResult> results,
        DetectionBenchmarkSummary summary
) {
    public DetectionBenchmarkReport {
        label = label == null || label.isBlank() ? "default" : label;
        results = List.copyOf(results == null ? List.of() : results);
        summary = summary == null ? DetectionBenchmarkSummary.empty() : summary;
    }
}


