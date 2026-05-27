package ru.eljke.driftguard.testkit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionBenchmarkMarkdownReportTest {
    @Test
    void rendersSummaryAndScenarioTable() {
        DetectionBenchmarkReport report = new DetectionBenchmarkReport(
                "balanced",
                List.of(new DetectionBenchmarkResult(
                        "latency-step",
                        new DetectionMetrics(2, 2, 0, 1, 1, 0, true, Duration.ofSeconds(3), 1.0, 1.0)
                )),
                new DetectionBenchmarkSummary(1, 1, 2, 2, 0, 1, 1, 0, 1.0, 1.0, Duration.ofSeconds(3))
        );

        String markdown = DetectionBenchmarkMarkdownReport.render(report);

        assertTrue(markdown.contains("# DriftGuard Benchmark: balanced"));
        assertTrue(markdown.contains("| Precision | 100.00% |"));
        assertTrue(markdown.contains("latency-step"));
        assertTrue(markdown.contains("3000 ms"));
    }
}
