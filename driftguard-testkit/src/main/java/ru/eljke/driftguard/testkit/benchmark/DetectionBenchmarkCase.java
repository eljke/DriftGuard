package ru.eljke.driftguard.testkit.benchmark;

import ru.eljke.driftguard.testkit.scenario.MetricScenario;

import java.util.Objects;

/**
 * Named benchmark case that pairs an id with a metric scenario.
 *
 * @param id stable benchmark or scenario identifier
 * @param scenario metric scenario under evaluation
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


