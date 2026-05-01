package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/**
 * Вычисляет простые метрики качества detector-а на synthetic scenario.
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
        boolean detected = intervals.stream()
                .anyMatch(interval -> events.stream().anyMatch(event -> interval.contains(event.detectedAt())));
        Duration delay = intervals.stream()
                .flatMap(interval -> events.stream()
                        .filter(event -> interval.contains(event.detectedAt()))
                        .min(Comparator.comparing(DriftEvent::detectedAt))
                        .map(event -> Duration.between(interval.start(), event.detectedAt()))
                        .stream())
                .findFirst()
                .orElse(null);
        return new DetectionMetrics(events.size(), truePositive, falsePositive, detected, delay);
    }
}
