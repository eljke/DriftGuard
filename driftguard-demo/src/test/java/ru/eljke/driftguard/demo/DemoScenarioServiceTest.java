package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.scenario.DemoRunResult;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoScenarioServiceTest {
    @Test
    void runLatencyDegradationProducesEvents() {
        DemoRunResult result = new DemoScenarioService(new DemoDetectionRuntime(), new SimpleMeterRegistry()).runLatencyDegradation();

        assertFalse(result.events().isEmpty());
        assertTrue(result.quality().detected());
        assertTrue(result.events().size() <= 1);
    }
}
