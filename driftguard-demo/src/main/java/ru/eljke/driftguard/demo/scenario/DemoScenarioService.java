package ru.eljke.driftguard.demo.scenario;

import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.demo.error.DemoErrorReason;
import ru.eljke.driftguard.demo.event.DemoDriftEventRepository;
import ru.eljke.driftguard.demo.event.InMemoryDemoDriftEventRepository;
import ru.eljke.driftguard.testkit.DetectionEvaluator;
import ru.eljke.driftguard.testkit.GradualDriftScenario;
import ru.eljke.driftguard.testkit.DetectionBenchmarkReport;
import ru.eljke.driftguard.testkit.DetectionBenchmarkResult;
import ru.eljke.driftguard.testkit.DetectionBenchmarkRunner;
import ru.eljke.driftguard.testkit.MetricScenario;
import ru.eljke.driftguard.testkit.PulseSpikeScenario;
import ru.eljke.driftguard.testkit.ScenarioConfig;
import ru.eljke.driftguard.testkit.SeasonalNoiseScenario;
import ru.eljke.driftguard.testkit.StepDriftScenario;
import ru.eljke.driftguard.testkit.ThroughputDropScenario;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * English demo documentation.
 * stores the result for the REST API.
 */
@Service
public class DemoScenarioService {
    private static final List<DemoScenarioDescriptor> SCENARIOS = List.of(
            new DemoScenarioDescriptor("latency-step", "Latency step degradation", "latency", "Sharp latency increase for the checkout endpoint."),
            new DemoScenarioDescriptor("error-rate-spike", "Error rate spike", "error-rate", "Short error-rate spike."),
            new DemoScenarioDescriptor("throughput-drop", "Throughput drop", "throughput", "Service throughput degradation."),
            new DemoScenarioDescriptor("queue-growth", "Queue backlog growth", "queue-size", "Gradual queue backlog growth."),
            new DemoScenarioDescriptor("seasonal-latency", "Seasonal latency", "latency", "Regular seasonality without expected drift."),
            new DemoScenarioDescriptor("microservices-system", "Microservices system", "mixed", "Several services publish latency, error rate and queue size at the same time.")
    );

    private final DemoDetectionRuntime runtime;
    private final MeterRegistry meterRegistry;
    private final DemoDriftEventRepository eventRepository;
    private final AtomicLong runSequence = new AtomicLong();
    private final ScheduledExecutorService playbackExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "driftguard-demo-playback");
        thread.setDaemon(true);
        return thread;
    });
    private volatile DemoRunResult lastResult;
    private volatile ScheduledFuture<?> playbackTask;

    @Autowired
    public DemoScenarioService(DemoDetectionRuntime runtime, MeterRegistry meterRegistry) {
        this(runtime, meterRegistry, new InMemoryDemoDriftEventRepository());
    }

    public DemoScenarioService(DemoDetectionRuntime runtime, MeterRegistry meterRegistry, DemoDriftEventRepository eventRepository) {
        this.runtime = runtime;
        this.meterRegistry = meterRegistry;
        this.eventRepository = eventRepository;
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
        return run(scenarioId, null);
    }

    public DemoRunResult run(String scenarioId, DemoScenarioRequest request) {
        stopLive();
        DemoScenarioDescriptor descriptor = findScenario(scenarioId);
        MetricScenario scenario = createScenario(
                descriptor.id(),
                "demo-run-" + runSequence.incrementAndGet(),
                safeRequest(descriptor.id(), request)
        );
        List<MetricPoint> points = scenario.generate();
        List<DriftEvent> events = new ArrayList<>();
        String runId = scenario.name();
        for (MetricPoint point : points) {
            List<DriftEvent> detected = runtime.detect(point);
            events.addAll(detected);
            eventRepository.appendAll("synthetic", runId, detected);
        }
        List<DriftEvent> representativeEvents = representativeEvents(scenario, events);
        recordRunMetrics(descriptor.id(), "instant", points.size(), representativeEvents.size());
        lastResult = new DemoRunResult(
                scenario.name(),
                descriptor.title(),
                "instant",
                false,
                points.size(),
                points.size(),
                points,
                scenario.expectedDrifts(),
                representativeEvents,
                DetectionEvaluator.evaluate(scenario, events)
        );
        return lastResult;
    }

    public synchronized DemoRunResult startLive(String scenarioId) {
        return startLive(scenarioId, null);
    }

    public synchronized DemoRunResult startLive(String scenarioId, DemoScenarioRequest request) {
        stopLive();
        DemoScenarioDescriptor descriptor = findScenario(scenarioId);
        MetricScenario scenario = createScenario(
                descriptor.id(),
                "live-run-" + runSequence.incrementAndGet(),
                safeRequest(descriptor.id(), request)
        );
        List<MetricPoint> points = scenario.generate();
        List<MetricPoint> processed = new ArrayList<>();
        List<DriftEvent> events = new ArrayList<>();
        String runId = scenario.name();
        AtomicInteger index = new AtomicInteger();
        lastResult = liveResult(scenario, descriptor, points, processed, events, true);
        playbackTask = playbackExecutor.scheduleAtFixedRate(new Runnable() {
            private int index;

            @Override
            public void run() {
                if (index >= points.size()) {
                    List<DriftEvent> representativeEvents = representativeEvents(scenario, events);
                    recordRunMetrics(descriptor.id(), "live", processed.size(), representativeEvents.size());
                    lastResult = liveResult(scenario, descriptor, points, processed, events, false);
                    stopLive();
                    return;
                }
                MetricPoint point = points.get(index++);
                processed.add(point);
                List<DriftEvent> detected = runtime.detect(point);
                events.addAll(detected);
                eventRepository.appendAll("live", runId, detected);
                List<DriftEvent> representativeEvents = representativeEvents(scenario, events);
                lastResult = new DemoRunResult(
                        scenario.name(),
                        descriptor.title(),
                        "live",
                        index < points.size(),
                        processed.size(),
                        points.size(),
                        List.copyOf(processed),
                        scenario.expectedDrifts(),
                        representativeEvents,
                        DetectionEvaluator.evaluate(scenario, events)
                );
            }
        }, 0, 120, TimeUnit.MILLISECONDS);
        return lastResult;
    }

    public DemoRunResult lastResult() {
        if (lastResult == null) {
            return run("latency-step");
        }
        return lastResult;
    }

    public DetectionBenchmarkReport benchmark() {
        stopLive();
        List<DetectionBenchmarkResult> results = new ArrayList<>();
        for (DemoScenarioDescriptor descriptor : SCENARIOS) {
            runtime.reset();
            MetricScenario scenario = createScenario(
                    descriptor.id(),
                    "benchmark-" + descriptor.id() + "-" + runSequence.incrementAndGet()
            );
            results.add(DetectionBenchmarkRunner.runScenario(descriptor.id(), scenario, runtime::detect));
        }
        runtime.reset();
        return DetectionBenchmarkRunner.report(runtime.profile().name(), results);
    }

    public List<DetectionBenchmarkReport> benchmarkProfiles() {
        stopLive();
        DemoDetectorProfile originalProfile = runtime.profile();
        List<DetectionBenchmarkReport> reports = new ArrayList<>();

        try {
            for (DemoDetectorProfile profile : DemoDetectorProfile.values()) {
                runtime.setProfile(profile);
                reports.add(benchmark());
            }
            return List.copyOf(reports);
        } finally {
            runtime.setProfile(originalProfile);
        }
    }

    public synchronized void stopLive() {
        ScheduledFuture<?> task = playbackTask;
        if (task != null) {
            task.cancel(false);
            playbackTask = null;
        }
    }

    private static DemoScenarioDescriptor findScenario(String scenarioId) {
        String id = scenarioId == null || scenarioId.isBlank() ? "latency-step" : scenarioId.trim();
        return SCENARIOS.stream()
                .filter(scenario -> scenario.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new DriftGuardValidationException(DemoErrorReason.UNKNOWN_SCENARIO, id));
    }

    public static MetricScenario createScenario(String scenarioId, String instance) {
        return createScenario(scenarioId, instance, safeRequest(scenarioId, null));
    }

    public static MetricScenario createScenario(String scenarioId, String instance, int samples) {
        return createScenario(scenarioId, instance, new DemoScenarioRequest(samples, null, null, null, null, null));
    }

    public static MetricScenario createScenario(String scenarioId, String instance, DemoScenarioRequest request) {
        DemoScenarioRequest safeRequest = safeRequest(scenarioId, request);
        int safeSamples = safeRequest.normalizedSamples(defaultSamples(scenarioId));
        double driftStartPercent = safeRequest.percentOrDefault(safeRequest.driftStartPercent(), defaultDriftStartPercent(scenarioId));
        double spikeLengthPercent = safeRequest.percentOrDefault(safeRequest.spikeLengthPercent(), 20.0);
        return switch (scenarioId) {
            case "latency-step" -> new StepDriftScenario(
                "latency-step-degradation",
                config("checkout-service", "latency", instance, "POST /checkout", MetricKind.DURATION, safeSamples),
                atPercent(safeSamples, driftStartPercent),
                safeRequest.valueOrDefault(safeRequest.baselineValue(), 100.0, 1.0, 10_000.0),
                safeRequest.valueOrDefault(safeRequest.driftValue(), 260.0, 1.0, 10_000.0),
                safeRequest.valueOrDefault(safeRequest.noiseStdDev(), 4.0, 0.0, 500.0)
            );
            case "error-rate-spike" -> new PulseSpikeScenario(
                    "error-rate-spike",
                    config("checkout-service", "error-rate", instance, "POST /checkout", MetricKind.RATE, safeSamples),
                    atPercent(safeSamples, driftStartPercent),
                    lengthPercent(safeSamples, driftStartPercent, spikeLengthPercent),
                    safeRequest.valueOrDefault(safeRequest.baselineValue(), 0.01, 0.0, 1.0),
                    safeRequest.valueOrDefault(safeRequest.driftValue(), 0.18, 0.0, 1.0),
                    safeRequest.valueOrDefault(safeRequest.noiseStdDev(), 0.002, 0.0, 0.5)
            );
            case "throughput-drop" -> new ThroughputDropScenario(
                    "throughput-drop",
                    config("checkout-service", "throughput", instance, "POST /checkout", MetricKind.RATE, safeSamples),
                    atPercent(safeSamples, driftStartPercent),
                    safeRequest.valueOrDefault(safeRequest.baselineValue(), 1000.0, 1.0, 1_000_000.0),
                    Math.min(
                            safeRequest.valueOrDefault(safeRequest.driftValue(), 430.0, 0.0, 1_000_000.0),
                            safeRequest.valueOrDefault(safeRequest.baselineValue(), 1000.0, 1.0, 1_000_000.0) - 0.001
                    ),
                    safeRequest.valueOrDefault(safeRequest.noiseStdDev(), 18.0, 0.0, 10_000.0)
            );
            case "queue-growth" -> new GradualDriftScenario(
                    "queue-backlog-growth",
                    config("orders-worker", "queue-size", instance, "orders.created", MetricKind.SIZE, safeSamples),
                    atPercent(safeSamples, driftStartPercent),
                    safeRequest.valueOrDefault(safeRequest.baselineValue(), 40.0, 0.0, 1_000_000.0),
                    safeRequest.valueOrDefault(safeRequest.driftValue(), 2.6, 0.0, 10_000.0),
                    safeRequest.valueOrDefault(safeRequest.noiseStdDev(), 4.0, 0.0, 10_000.0)
            );
            case "seasonal-latency" -> new SeasonalNoiseScenario(
                    "seasonal-latency",
                    config("checkout-service", "latency", instance, "POST /checkout", MetricKind.DURATION, safeSamples),
                    safeRequest.valueOrDefault(safeRequest.baselineValue(), 120.0, 1.0, 10_000.0),
                    safeRequest.valueOrDefault(safeRequest.driftValue(), 25.0, 0.0, 10_000.0),
                    Math.max(20, at(safeSamples, 0.25)),
                    safeRequest.valueOrDefault(safeRequest.noiseStdDev(), 2.0, 0.0, 500.0)
            );
            case "microservices-system" -> new StepDriftScenario(
                    "microservices-system-latency",
                    config("checkout-service", "latency", instance, "POST /checkout", MetricKind.DURATION, safeSamples),
                    atPercent(safeSamples, driftStartPercent),
                    safeRequest.valueOrDefault(safeRequest.baselineValue(), 100.0, 1.0, 10_000.0),
                    safeRequest.valueOrDefault(safeRequest.driftValue(), 260.0, 1.0, 10_000.0),
                    safeRequest.valueOrDefault(safeRequest.noiseStdDev(), 4.0, 0.0, 500.0)
            );
            default -> throw new DriftGuardValidationException(DemoErrorReason.UNKNOWN_SCENARIO, scenarioId);
        };
    }

    public static ScenarioConfig config(String service, String metric, String instance, String operation, MetricKind kind, int samples) {
        return new ScenarioConfig(
                new MetricKey(service, metric, instance, operation),
                kind,
                Instant.now().minusSeconds(samples),
                Duration.ofSeconds(1),
                samples,
                42L
        );
    }

    private static DemoRunResult liveResult(
            MetricScenario scenario,
            DemoScenarioDescriptor descriptor,
            List<MetricPoint> points,
            List<MetricPoint> processed,
            List<DriftEvent> events,
            boolean running
    ) {
        List<DriftEvent> representativeEvents = representativeEvents(scenario, events);
        return new DemoRunResult(
                scenario.name(),
                descriptor.title(),
                "live",
                running,
                processed.size(),
                points.size(),
                List.copyOf(processed),
                scenario.expectedDrifts(),
                representativeEvents,
                DetectionEvaluator.evaluate(scenario, events)
        );
    }

    private static List<DriftEvent> representativeEvents(MetricScenario scenario, List<DriftEvent> events) {
        Map<String, DriftEvent> byDetector = new LinkedHashMap<>();
        for (DriftEvent event : events) {
            String key = representativeKey(event);
            DriftEvent current = byDetector.get(key);
            if (current == null || shouldReplaceRepresentative(scenario, current, event)) {
                byDetector.put(key, event);
            }
        }
        return List.copyOf(byDetector.values());
    }

    private static String representativeKey(DriftEvent event) {
        return event.key().service() + "|" + event.key().metric() + "|" + event.key().operation() + "|" + event.detector();
    }

    private static boolean shouldReplaceRepresentative(MetricScenario scenario, DriftEvent current, DriftEvent candidate) {
        return !isInsideExpectedDrift(scenario, current) && isInsideExpectedDrift(scenario, candidate);
    }

    private static boolean isInsideExpectedDrift(MetricScenario scenario, DriftEvent event) {
        return scenario.expectedDrifts().stream()
                .anyMatch(interval -> interval.contains(event.detectedAt()));
    }

    private void recordRunMetrics(String scenario, String mode, int points, int events) {
        Counter.builder("driftguard.demo.scenario.runs")
                .tag("scenario", scenario)
                .tag("mode", mode)
                .register(meterRegistry)
                .increment();
        Counter.builder("driftguard.demo.metric.points")
                .tag("scenario", scenario)
                .tag("mode", mode)
                .register(meterRegistry)
                .increment(points);
        Counter.builder("driftguard.demo.drift.events")
                .tag("scenario", scenario)
                .tag("mode", mode)
                .register(meterRegistry)
                .increment(events);
    }

    private static DemoScenarioRequest safeRequest(String scenarioId, DemoScenarioRequest request) {
        if (request == null) {
            return new DemoScenarioRequest(defaultSamples(scenarioId), null, null, null, null, null);
        }
        return request;
    }

    private static int defaultSamples(String scenarioId) {
        return switch (scenarioId) {
            case "error-rate-spike" -> 140;
            case "throughput-drop" -> 150;
            case "seasonal-latency" -> 180;
            default -> 160;
        };
    }

    private static int at(int samples, double ratio) {
        return Math.max(1, Math.min(samples - 2, (int) Math.round(samples * ratio)));
    }

    private static int atPercent(int samples, double percent) {
        return at(samples, percent / 100.0);
    }

    private static int lengthPercent(int samples, double startPercent, double lengthPercent) {
        int start = atPercent(samples, startPercent);
        return Math.max(8, Math.min(samples - start, (int) Math.round(samples * lengthPercent / 100.0)));
    }

    private static double defaultDriftStartPercent(String scenarioId) {
        return switch (scenarioId) {
            case "error-rate-spike" -> 43.0;
            case "throughput-drop" -> 47.0;
            case "queue-growth" -> 34.0;
            default -> 50.0;
        };
    }
}


