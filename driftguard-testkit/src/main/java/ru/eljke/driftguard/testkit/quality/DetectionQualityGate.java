package ru.eljke.driftguard.testkit.quality;

import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkReport;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkSummary;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Set of minimum detection quality requirements.
 *
 * <p>Quality gates are used in tests and CI so algorithm changes
 * Thresholds that define acceptable benchmark quality.
 * All thresholds are optional: when a value is absent, the corresponding check
 * is skipped.</p>
 */
public record DetectionQualityGate(
        Double minPrecision,
        Double minRecall,
        Integer maxFalsePositiveEvents,
        Integer maxMissedDriftIntervals,
        Duration maxMeanFirstDetectionDelay
) {
    public DetectionQualityGate {
        validateProbability(minPrecision, "minPrecision");
        validateProbability(minRecall, "minRecall");
        validateNonNegative(maxFalsePositiveEvents, "maxFalsePositiveEvents");
        validateNonNegative(maxMissedDriftIntervals, "maxMissedDriftIntervals");
        if (maxMeanFirstDetectionDelay != null && maxMeanFirstDetectionDelay.isNegative()) {
            throw new IllegalArgumentException("maxMeanFirstDetectionDelay must not be negative");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public DetectionQualityReport evaluate(DetectionBenchmarkReport report) {
        Objects.requireNonNull(report, "report must not be null");
        DetectionBenchmarkSummary summary = report.summary();
        List<DetectionQualityViolation> violations = new ArrayList<>();

        if (minPrecision != null && summary.precision() < minPrecision) {
            violations.add(DetectionQualityViolation.below("precision", minPrecision, summary.precision()));
        }
        if (minRecall != null && summary.recall() < minRecall) {
            violations.add(DetectionQualityViolation.below("recall", minRecall, summary.recall()));
        }
        if (maxFalsePositiveEvents != null && summary.falsePositiveEvents() > maxFalsePositiveEvents) {
            violations.add(DetectionQualityViolation.above(
                    "falsePositiveEvents",
                    maxFalsePositiveEvents,
                    summary.falsePositiveEvents()
            ));
        }
        if (maxMissedDriftIntervals != null && summary.missedDriftIntervals() > maxMissedDriftIntervals) {
            violations.add(DetectionQualityViolation.above(
                    "missedDriftIntervals",
                    maxMissedDriftIntervals,
                    summary.missedDriftIntervals()
            ));
        }
        if (maxMeanFirstDetectionDelay != null
                && summary.meanFirstDetectionDelay() != null
                && summary.meanFirstDetectionDelay().compareTo(maxMeanFirstDetectionDelay) > 0) {
            violations.add(DetectionQualityViolation.above(
                    "meanFirstDetectionDelay",
                    maxMeanFirstDetectionDelay,
                    summary.meanFirstDetectionDelay()
            ));
        }

        return new DetectionQualityReport(report, this, violations);
    }

    public void assertPassed(DetectionBenchmarkReport report) {
        DetectionQualityReport qualityReport = evaluate(report);
        if (!qualityReport.passed()) {
            throw new DetectionQualityGateException(qualityReport);
        }
    }

    private static void validateProbability(Double value, String field) {
        if (value != null && (value < 0.0 || value > 1.0)) {
            throw new IllegalArgumentException(field + " must be between 0.0 and 1.0");
        }
    }

    private static void validateNonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }

    public static final class Builder {
        private Double minPrecision;
        private Double minRecall;
        private Integer maxFalsePositiveEvents;
        private Integer maxMissedDriftIntervals;
        private Duration maxMeanFirstDetectionDelay;

        private Builder() {
        }

        public Builder minPrecision(double minPrecision) {
            this.minPrecision = minPrecision;
            return this;
        }

        public Builder minRecall(double minRecall) {
            this.minRecall = minRecall;
            return this;
        }

        public Builder maxFalsePositiveEvents(int maxFalsePositiveEvents) {
            this.maxFalsePositiveEvents = maxFalsePositiveEvents;
            return this;
        }

        public Builder maxMissedDriftIntervals(int maxMissedDriftIntervals) {
            this.maxMissedDriftIntervals = maxMissedDriftIntervals;
            return this;
        }

        public Builder maxMeanFirstDetectionDelay(Duration maxMeanFirstDetectionDelay) {
            this.maxMeanFirstDetectionDelay = maxMeanFirstDetectionDelay;
            return this;
        }

        public DetectionQualityGate build() {
            return new DetectionQualityGate(
                    minPrecision,
                    minRecall,
                    maxFalsePositiveEvents,
                    maxMissedDriftIntervals,
                    maxMeanFirstDetectionDelay
            );
        }
    }
}


