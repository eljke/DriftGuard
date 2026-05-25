package ru.eljke.driftguard.testkit;

import java.time.Duration;

/**
 * English API documentation.
 *
 * @param events documented value
 * @param truePositiveEvents documented value
 * @param falsePositiveEvents documented value
 * @param expectedDriftIntervals documented value
 * @param detectedDriftIntervals documented value
 * @param missedDriftIntervals documented value
 * @param detected documented value
 * @param firstDetectionDelay documented value
 * @param precision documented value
 * @param recall documented value
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


