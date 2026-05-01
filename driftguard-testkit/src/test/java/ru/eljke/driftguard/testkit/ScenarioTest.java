package ru.eljke.driftguard.testkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ScenarioTest {
    @Test
    void stepScenarioGeneratesExpectedDriftInterval() {
        MetricScenario scenario = new StepDriftScenario(
                "latency-step",
                ScenarioConfig.latency("orders", "POST /orders", 100),
                50,
                100.0,
                220.0,
                2.0
        );

        assertEquals(100, scenario.generate().size());
        assertEquals(1, scenario.expectedDrifts().size());
        assertFalse(scenario.expectedDrifts().getFirst().start().isBefore(scenario.generate().get(50).timestamp()));
    }
}
