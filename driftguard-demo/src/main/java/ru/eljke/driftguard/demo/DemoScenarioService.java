package ru.eljke.driftguard.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.DetectionEvaluator;
import ru.eljke.driftguard.testkit.MetricScenario;
import ru.eljke.driftguard.testkit.ScenarioConfig;
import ru.eljke.driftguard.testkit.StepDriftScenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Запускает synthetic scenario через настоящий DriftGuard engine и хранит
 * результат для REST API.
 */
@Service
public class DemoScenarioService {
    private final DriftDetectorEngine engine;
    private volatile DemoRunResult lastResult;

    public DemoScenarioService(DriftDetectorEngine engine) {
        this.engine = engine;
    }

    @PostConstruct
    public void runOnStartup() {
        runLatencyDegradation();
    }

    public DemoRunResult runLatencyDegradation() {
        MetricScenario scenario = new StepDriftScenario(
                "latency-step-degradation",
                ScenarioConfig.latency("checkout-service", "POST /checkout", 160),
                80,
                100.0,
                260.0,
                4.0
        );
        List<MetricPoint> points = scenario.generate();
        List<DriftEvent> events = new ArrayList<>();
        for (MetricPoint point : points) {
            events.addAll(engine.detect(point));
        }
        lastResult = new DemoRunResult(
                scenario.name(),
                points.size(),
                points.stream().skip(Math.max(0, points.size() - 20L)).toList(),
                List.copyOf(events),
                DetectionEvaluator.evaluate(scenario, events)
        );
        return lastResult;
    }

    public DemoRunResult lastResult() {
        if (lastResult == null) {
            return runLatencyDegradation();
        }
        return lastResult;
    }
}
