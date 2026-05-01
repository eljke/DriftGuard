package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoScenarioServiceTest {
    @Test
    void runLatencyDegradationProducesEvents() {
        DriftDetectorEngine engine = new DriftDetectorEngine(
                DefaultAlgorithms.registry(),
                new InMemoryDetectorStateStore(),
                List.of(
                        new DetectorDefinition("latency-page-hinkley", new PageHinkleyConfig(20, 0.1, 25.0, 50.0, 0.05), key -> key.metric().equals("latency"))
                )
        );

        DemoRunResult result = new DemoScenarioService(engine, new SimpleMeterRegistry()).runLatencyDegradation();

        assertFalse(result.events().isEmpty());
        assertTrue(result.quality().detected());
        assertTrue(result.events().size() <= 1);
    }
}
