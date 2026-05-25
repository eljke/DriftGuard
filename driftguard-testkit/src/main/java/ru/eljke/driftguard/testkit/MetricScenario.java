package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Synthetic metric scenario used for detector benchmarks and demos.
 */
public interface MetricScenario {
    /**
     * Synthetic metric scenario used for detector benchmarks and demos.
     */
    String name();

    /**
     * Synthetic metric scenario used for detector benchmarks and demos.
     */
    List<MetricPoint> generate();

    /**
     * Synthetic metric scenario used for detector benchmarks and demos.
     */
    List<DriftInterval> expectedDrifts();
}


