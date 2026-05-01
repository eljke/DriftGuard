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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        List<DriftEvent> representativeEvents = firstEventPerDetector(events);
        lastResult = new DemoRunResult(
                scenario.name(),
                points.size(),
                points.stream().skip(Math.max(0, points.size() - 20L)).toList(),
                representativeEvents,
                DetectionEvaluator.evaluate(scenario, representativeEvents)
        );
        return lastResult;
    }

    public DemoRunResult lastResult() {
        if (lastResult == null) {
            return runLatencyDegradation();
        }
        return lastResult;
    }

    private static List<DriftEvent> firstEventPerDetector(List<DriftEvent> events) {
        Map<String, DriftEvent> byDetector = new LinkedHashMap<>();
        for (DriftEvent event : events) {
            byDetector.putIfAbsent(event.detector(), event);
        }
        return List.copyOf(byDetector.values());
    }
}
