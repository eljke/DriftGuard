package ru.eljke.driftguard.testkit.benchmark;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.testkit.scenario.DriftInterval;
import ru.eljke.driftguard.testkit.scenario.MetricScenario;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * Evaluates emitted events against expected drift intervals.
 */
public final class DetectionEvaluator {
    private DetectionEvaluator() {
    }

    public static DetectionMetrics evaluate(MetricScenario scenario, List<DriftEvent> events) {
        List<DriftInterval> intervals = scenario.expectedDrifts();
        int truePositive = 0;
        int falsePositive = 0;
        for (DriftEvent event : events) {
            boolean insideExpected = intervals.stream().anyMatch(interval -> interval.contains(event.detectedAt()));
            if (insideExpected) {
                truePositive++;
            } else {
                falsePositive++;
            }
        }
        int detectedIntervals = (int) intervals.stream()
                .filter(interval -> events.stream().anyMatch(event -> interval.contains(event.detectedAt())))
                .count();
        boolean detected = detectedIntervals > 0;
        Duration delay = intervals.stream()
                .flatMap(interval -> events.stream()
                        .filter(event -> interval.contains(event.detectedAt()))
                        .min(Comparator.comparing(DriftEvent::detectedAt))
                        .map(event -> Duration.between(interval.start(), event.detectedAt()))
                        .stream())
                .findFirst()
                .orElse(null);
        double precision = events.isEmpty() ? 1.0 : (double) truePositive / events.size();
        double recall = intervals.isEmpty() ? 1.0 : (double) detectedIntervals / intervals.size();
        return new DetectionMetrics(
                events.size(),
                truePositive,
                falsePositive,
                intervals.size(),
                detectedIntervals,
                intervals.size() - detectedIntervals,
                detected,
                delay,
                precision,
                recall
        );
    }
}


