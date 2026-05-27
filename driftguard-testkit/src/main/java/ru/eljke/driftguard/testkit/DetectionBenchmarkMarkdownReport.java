package ru.eljke.driftguard.testkit;

import java.time.Duration;
import java.util.Locale;

/**
 * Renders benchmark reports as Markdown for release notes, pull requests and
 * academic evaluation appendices.
 */
public final class DetectionBenchmarkMarkdownReport {
    private DetectionBenchmarkMarkdownReport() {
    }

    public static String render(DetectionBenchmarkReport report) {
        DetectionBenchmarkReport safeReport = report == null
                ? new DetectionBenchmarkReport("default", java.util.List.of(), DetectionBenchmarkSummary.empty())
                : report;
        StringBuilder markdown = new StringBuilder();
        appendSummary(markdown, safeReport);
        appendResults(markdown, safeReport);
        return markdown.toString();
    }

    private static void appendSummary(StringBuilder markdown, DetectionBenchmarkReport report) {
        DetectionBenchmarkSummary summary = report.summary();
        markdown.append("# DriftGuard Benchmark: ").append(report.label()).append("\n\n");
        markdown.append("| Metric | Value |\n");
        markdown.append("|---|---:|\n");
        row(markdown, "Scenarios", summary.scenarios());
        row(markdown, "Detected scenarios", summary.detectedScenarios());
        row(markdown, "Events", summary.events());
        row(markdown, "True positives", summary.truePositiveEvents());
        row(markdown, "False positives", summary.falsePositiveEvents());
        row(markdown, "Expected intervals", summary.expectedDriftIntervals());
        row(markdown, "Detected intervals", summary.detectedDriftIntervals());
        row(markdown, "Missed intervals", summary.missedDriftIntervals());
        row(markdown, "Precision", percent(summary.precision()));
        row(markdown, "Recall", percent(summary.recall()));
        row(markdown, "Mean first detection delay", duration(summary.meanFirstDetectionDelay()));
        markdown.append("\n");
    }

    private static void appendResults(StringBuilder markdown, DetectionBenchmarkReport report) {
        markdown.append("| Scenario | Detected | Events | TP | FP | Missed | Precision | Recall | First delay |\n");
        markdown.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (DetectionBenchmarkResult result : report.results()) {
            DetectionMetrics metrics = result.metrics();
            markdown.append("| ")
                    .append(escape(result.scenario()))
                    .append(" | ")
                    .append(metrics.detected() ? "yes" : "no")
                    .append(" | ")
                    .append(metrics.events())
                    .append(" | ")
                    .append(metrics.truePositiveEvents())
                    .append(" | ")
                    .append(metrics.falsePositiveEvents())
                    .append(" | ")
                    .append(metrics.missedDriftIntervals())
                    .append(" | ")
                    .append(percent(metrics.precision()))
                    .append(" | ")
                    .append(percent(metrics.recall()))
                    .append(" | ")
                    .append(duration(metrics.firstDetectionDelay()))
                    .append(" |\n");
        }
    }

    private static void row(StringBuilder markdown, String metric, Object value) {
        markdown.append("| ").append(metric).append(" | ").append(value).append(" |\n");
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0);
    }

    private static String duration(Duration duration) {
        if (duration == null) {
            return "-";
        }
        return duration.toMillis() + " ms";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|");
    }
}
