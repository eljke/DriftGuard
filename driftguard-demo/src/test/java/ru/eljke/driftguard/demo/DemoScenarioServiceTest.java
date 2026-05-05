package ru.eljke.driftguard.demo;

import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftEventPhase;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.scenario.DemoRunResult;
import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.testkit.MetricScenario;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void errorRateSpikeProducesRecoveryAfterReturningToBaseline() {
        DemoDetectionRuntime runtime = new DemoDetectionRuntime();
        MetricScenario scenario = DemoScenarioService.createScenario("error-rate-spike", "test-error-rate-spike");
        List<DriftEvent> events = new ArrayList<>();

        for (MetricPoint point : scenario.generate()) {
            events.addAll(runtime.detect(point));
        }

        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(event -> event.key().metric().equals("error-rate")));
        assertTrue(events.stream().anyMatch(event -> event.phase() == DriftEventPhase.RECOVERED));
    }

    @Test
    void runScenarioUsesRequestedSampleCount() {
        DemoScenarioService service = new DemoScenarioService(new DemoDetectionRuntime(), new SimpleMeterRegistry());

        DemoRunResult result = service.run("latency-step", new DemoScenarioRequest(240));

        assertEquals(240, result.metricPoints());
        assertEquals(240, result.samplePoints().size());
    }
}
