package ru.eljke.driftguard.testkit.benchmark;

import java.time.Duration;

/**
 * Precision, recall and timing metrics for detector evaluation.
 *
 * @param events emitted drift events
 * @param truePositiveEvents events inside expected drift intervals
 * @param falsePositiveEvents events outside expected drift intervals
 * @param expectedDriftIntervals number of expected drift intervals
 * @param detectedDriftIntervals expected intervals with at least one detection
 * @param missedDriftIntervals expected intervals without detections
 * @param detected whether any expected interval was detected
 * @param firstDetectionDelay delay before the first true positive detection
 * @param precision share of emitted events that were true positives
 * @param recall share of expected intervals that were detected
 */
public record DetectionMetrics(
        int events,
        int truePositiveEvents,
        int falsePositiveEvents,
        int expectedDriftIntervals,
        int detectedDriftIntervals,
        int missedDriftIntervals,
        boolean detected,
        Duration firstDetectionDelay,
        double precision,
        double recall
) {
}


