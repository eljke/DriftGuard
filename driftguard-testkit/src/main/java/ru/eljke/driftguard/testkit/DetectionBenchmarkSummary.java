package ru.eljke.driftguard.testkit;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Агрегированные метрики качества по нескольким synthetic scenarios.
 */
public record DetectionBenchmarkSummary(
        int scenarios,
        int detectedScenarios,
        int events,
        int truePositiveEvents,
        int falsePositiveEvents,
        int expectedDriftIntervals,
        int detectedDriftIntervals,
        int missedDriftIntervals,
        double precision,
        double recall,
        Duration meanFirstDetectionDelay
) {
    public static DetectionBenchmarkSummary empty() {
        return new DetectionBenchmarkSummary(0, 0, 0, 0, 0, 0, 0, 0, 1.0, 1.0, Duration.ZERO);
    }

    public static DetectionBenchmarkSummary from(List<DetectionBenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            return empty();
        }

        int scenarios = results.size();
        int detectedScenarios = 0;
        int events = 0;
        int truePositiveEvents = 0;
        int falsePositiveEvents = 0;
        int expectedDriftIntervals = 0;
        int detectedDriftIntervals = 0;
        int missedDriftIntervals = 0;

        for (DetectionBenchmarkResult result : results) {
            DetectionMetrics metrics = result.metrics();
            if (metrics.detected()) {
                detectedScenarios++;
            }
            events += metrics.events();
            truePositiveEvents += metrics.truePositiveEvents();
            falsePositiveEvents += metrics.falsePositiveEvents();
            expectedDriftIntervals += metrics.expectedDriftIntervals();
            detectedDriftIntervals += metrics.detectedDriftIntervals();
            missedDriftIntervals += metrics.missedDriftIntervals();
        }

        double precision = events == 0 ? 1.0 : (double) truePositiveEvents / events;
        double recall = expectedDriftIntervals == 0 ? 1.0 : (double) detectedDriftIntervals / expectedDriftIntervals;

        return new DetectionBenchmarkSummary(
                scenarios,
                detectedScenarios,
                events,
                truePositiveEvents,
                falsePositiveEvents,
                expectedDriftIntervals,
                detectedDriftIntervals,
                missedDriftIntervals,
                precision,
                recall,
                meanDelay(results)
        );
    }

    private static Duration meanDelay(List<DetectionBenchmarkResult> results) {
        List<Duration> delays = results.stream()
                .map(DetectionBenchmarkResult::metrics)
                .map(DetectionMetrics::firstDetectionDelay)
                .filter(Objects::nonNull)
                .toList();

        if (delays.isEmpty()) {
            return Duration.ZERO;
        }

        long millis = delays.stream()
                .mapToLong(Duration::toMillis)
                .sum() / delays.size();

        return Duration.ofMillis(millis);
    }
}