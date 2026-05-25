package ru.eljke.driftguard.testkit;

import java.util.List;

/**
 * English API documentation.
 *
 * @param label documented value
 * @param results documented value
 * @param summary documented value
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


