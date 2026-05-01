package ru.eljke.driftguard.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.DetectionEvaluator;
import ru.eljke.driftguard.testkit.DriftInterval;
import ru.eljke.driftguard.testkit.GradualDriftScenario;
import ru.eljke.driftguard.testkit.MetricScenario;
import ru.eljke.driftguard.testkit.PulseSpikeScenario;
import ru.eljke.driftguard.testkit.ScenarioConfig;
import ru.eljke.driftguard.testkit.SeasonalNoiseScenario;
import ru.eljke.driftguard.testkit.StepDriftScenario;
import ru.eljke.driftguard.testkit.ThroughputDropScenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Запускает synthetic scenario через настоящий DriftGuard engine и хранит
 * результат для REST API.
 */
@Service
public class DemoScenarioService {
    private static final List<DemoScenarioDescriptor> SCENARIOS = List.of(
            new DemoScenarioDescriptor("latency-step", "Latency step degradation", "latency", "Резкий рост latency checkout endpoint-а."),
            new DemoScenarioDescriptor("error-rate-spike", "Error rate spike", "error-rate", "Кратковременный всплеск доли ошибок."),
            new DemoScenarioDescriptor("throughput-drop", "Throughput drop", "throughput", "Падение пропускной способности сервиса."),
            new DemoScenarioDescriptor("queue-growth", "Queue backlog growth", "queue-size", "Плавный рост размера очереди."),
            new DemoScenarioDescriptor("seasonal-latency", "Seasonal latency", "latency", "Регулярная сезонность без ожидаемого drift-а.")
    );

    private final DriftDetectorEngine engine;
    private final AtomicLong runSequence = new AtomicLong();
    private volatile DemoRunResult lastResult;

    public DemoScenarioService(DriftDetectorEngine engine) {
        this.engine = engine;
    }

    @PostConstruct
    public void runOnStartup() {
        run("latency-step");
    }

    public DemoRunResult runLatencyDegradation() {
        return run("latency-step");
    }

    public List<DemoScenarioDescriptor> scenarios() {
        return SCENARIOS;
    }

    public DemoRunResult run(String scenarioId) {
        DemoScenarioDescriptor descriptor = findScenario(scenarioId);
        MetricScenario scenario = createScenario(descriptor.id(), "demo-run-" + runSequence.incrementAndGet());
        List<MetricPoint> points = scenario.generate();
        List<DriftEvent> events = new ArrayList<>();
        for (MetricPoint point : points) {
            events.addAll(engine.detect(point));
        }
        List<DriftEvent> representativeEvents = firstExpectedEventPerDetector(events, scenario.expectedDrifts());
        lastResult = new DemoRunResult(
                scenario.name(),
                descriptor.title(),
                points.size(),
                points,
                scenario.expectedDrifts(),
                representativeEvents,
                DetectionEvaluator.evaluate(scenario, representativeEvents)
        );
        return lastResult;
    }

    public DemoRunResult lastResult() {
        if (lastResult == null) {
            return run("latency-step");
        }
        return lastResult;
    }

    private static DemoScenarioDescriptor findScenario(String scenarioId) {
        String id = scenarioId == null || scenarioId.isBlank() ? "latency-step" : scenarioId.trim();
        return SCENARIOS.stream()
                .filter(scenario -> scenario.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown demo scenario: " + id));
    }

    private static MetricScenario createScenario(String scenarioId, String instance) {
        return switch (scenarioId) {
            case "latency-step" -> new StepDriftScenario(
                "latency-step-degradation",
                config("checkout-service", "latency", instance, "POST /checkout", MetricKind.DURATION, 160),
                80,
                100.0,
                260.0,
                4.0
            );
            case "error-rate-spike" -> new PulseSpikeScenario(
                    "error-rate-spike",
                    config("checkout-service", "error-rate", instance, "POST /checkout", MetricKind.RATE, 140),
                    60,
                    28,
                    0.01,
                    0.18,
                    0.002
            );
            case "throughput-drop" -> new ThroughputDropScenario(
                    "throughput-drop",
                    config("checkout-service", "throughput", instance, "POST /checkout", MetricKind.RATE, 150),
                    70,
                    1000.0,
                    430.0,
                    18.0
            );
            case "queue-growth" -> new GradualDriftScenario(
                    "queue-backlog-growth",
                    config("orders-worker", "queue-size", instance, "orders.created", MetricKind.SIZE, 160),
                    55,
                    40.0,
                    2.6,
                    4.0
            );
            case "seasonal-latency" -> new SeasonalNoiseScenario(
                    "seasonal-latency",
                    config("checkout-service", "latency", instance, "POST /checkout", MetricKind.DURATION, 180),
                    120.0,
                    25.0,
                    45,
                    2.0
            );
            default -> throw new IllegalArgumentException("Unknown demo scenario: " + scenarioId);
        };
    }

    private static ScenarioConfig config(String service, String metric, String instance, String operation, MetricKind kind, int samples) {
        return new ScenarioConfig(
                new MetricKey(service, metric, instance, operation),
                kind,
                java.time.Instant.parse("2026-05-01T10:00:00Z"),
                java.time.Duration.ofSeconds(1),
                samples,
                42L
        );
    }

    private static List<DriftEvent> firstExpectedEventPerDetector(List<DriftEvent> events, List<DriftInterval> expectedDrifts) {
        if (expectedDrifts.isEmpty()) {
            return List.of();
        }
        Map<String, DriftEvent> byDetector = new LinkedHashMap<>();
        for (DriftEvent event : events) {
            if (expectedDrifts.stream().anyMatch(interval -> interval.contains(event.detectedAt()))) {
                byDetector.putIfAbsent(event.detector(), event);
            }
        }
        return List.copyOf(byDetector.values());
    }
}
