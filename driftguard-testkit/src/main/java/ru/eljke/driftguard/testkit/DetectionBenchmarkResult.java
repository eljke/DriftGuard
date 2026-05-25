package ru.eljke.driftguard.testkit;

/**
 * English API documentation.
 *
 * @param scenario documented value
 * @param metrics documented value
 */
public record DetectionBenchmarkResult(
        String scenario,
        DetectionMetrics metrics
) {
}


