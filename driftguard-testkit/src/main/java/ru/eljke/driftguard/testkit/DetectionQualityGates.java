package ru.eljke.driftguard.testkit;

import java.time.Duration;

/**
 * English API documentation.
 *
 * English API documentation.
 * English API documentation.
 */
public final class DetectionQualityGates {
    private DetectionQualityGates() {
    }

    /**
     * English API documentation.
     */
    public static DetectionQualityGate smoke() {
        return DetectionQualityGate.builder()
                .minRecall(0.5)
                .maxMissedDriftIntervals(1)
                .build();
    }

    /**
     * English API documentation.
     */
    public static DetectionQualityGate balanced() {
        return DetectionQualityGate.builder()
                .minPrecision(0.75)
                .minRecall(0.75)
                .maxFalsePositiveEvents(3)
                .maxMissedDriftIntervals(1)
                .maxMeanFirstDetectionDelay(Duration.ofMinutes(2))
                .build();
    }

    /**
     * English API documentation.
     */
    public static DetectionQualityGate strict() {
        return DetectionQualityGate.builder()
                .minPrecision(0.9)
                .minRecall(0.9)
                .maxFalsePositiveEvents(1)
                .maxMissedDriftIntervals(0)
                .maxMeanFirstDetectionDelay(Duration.ofSeconds(45))
                .build();
    }
}


