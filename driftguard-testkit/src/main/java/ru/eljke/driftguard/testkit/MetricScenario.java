package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * English API documentation.
 */
public interface MetricScenario {
    /**
     * English API documentation.
     */
    String name();

    /**
     * English API documentation.
     */
    List<MetricPoint> generate();

    /**
     * English API documentation.
     */
    List<DriftInterval> expectedDrifts();
}


