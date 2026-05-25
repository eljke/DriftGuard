package ru.eljke.driftguard.testkit;

/**
 * Quality result for one benchmark scenario.
 *
 * @param scenario metric scenario under evaluation
 * @param metrics calculated detection metrics
 */
public record DetectionBenchmarkResult(
        String scenario,
        DetectionMetrics metrics
) {
}


