package ru.eljke.driftguard.testkit;

import java.util.Objects;

/**
 * English API documentation.
 *
 * @param id documented value
 * @param scenario documented value
 */
public record DetectionBenchmarkCase(
        String id,
        MetricScenario scenario
) {
    public DetectionBenchmarkCase {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    }

    public static DetectionBenchmarkCase of(String id, MetricScenario scenario) {
        return new DetectionBenchmarkCase(id, scenario);
    }
}


