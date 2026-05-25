package ru.eljke.driftguard.demo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.demo.capability.DemoCapabilityGroup;
import ru.eljke.driftguard.demo.capability.DemoCapabilityService;
import ru.eljke.driftguard.demo.config.DemoConfigurationService;
import ru.eljke.driftguard.demo.config.DemoConfigurationView;
import ru.eljke.driftguard.demo.config.DemoToolProperties;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.demo.error.DemoErrorReason;
import ru.eljke.driftguard.demo.kafka.KafkaDemoService;
import ru.eljke.driftguard.demo.kafka.KafkaDemoStatus;
import ru.eljke.driftguard.demo.kafka.KafkaReplayRequest;
import ru.eljke.driftguard.demo.kafka.ops.KafkaOperationsService;
import ru.eljke.driftguard.demo.kafka.ops.KafkaOperationsSnapshot;
import ru.eljke.driftguard.demo.event.DemoDriftEventRepository;
import ru.eljke.driftguard.demo.event.DemoStoredDriftEvent;
import ru.eljke.driftguard.demo.scenario.DemoRunResult;
import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;
import ru.eljke.driftguard.demo.scenario.DemoScenarioDescriptor;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.demo.tool.ToolLink;
import ru.eljke.driftguard.testkit.DetectionBenchmarkReport;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "DriftGuard demo REST API")
public class DemoController {
    private final DemoCapabilityService capabilityService;
    private final DemoScenarioService service;
    private final KafkaDemoService kafkaDemoService;
    private final KafkaOperationsService kafkaOperationsService;
    private final DemoToolProperties toolProperties;
    private final DemoConfigurationService configurationService;
    private final DemoDriftEventRepository eventRepository;

    public DemoController(
            DemoCapabilityService capabilityService,
            DemoScenarioService service,
            KafkaDemoService kafkaDemoService,
            KafkaOperationsService kafkaOperationsService,
            DemoToolProperties toolProperties,
            DemoConfigurationService configurationService,
            DemoDriftEventRepository eventRepository
    ) {
        this.capabilityService = capabilityService;
        this.service = service;
        this.kafkaDemoService = kafkaDemoService;
        this.kafkaOperationsService = kafkaOperationsService;
        this.toolProperties = toolProperties;
        this.configurationService = configurationService;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoRunResult overview() {
        return service.lastResult();
    }

    @GetMapping("/events")
    @Operation(summary = "DriftGuard demo endpoint")
    public List<DriftEvent> events() {
        return eventRepository.recentEvents(200);
    }

    @GetMapping("/events/stored")
    @Operation(summary = "DriftGuard demo endpoint")
    public List<DemoStoredDriftEvent> storedEvents() {
        return eventRepository.recent(200);
    }

    @PostMapping("/events/clear")
    @Operation(summary = "DriftGuard demo endpoint")
    public Map<String, Object> clearStoredEvents() {
        eventRepository.clear();
        return Map.of("cleared", true);
    }

    @GetMapping("/quality")
    @Operation(summary = "DriftGuard demo endpoint")
    public Object quality() {
        return service.lastResult().quality();
    }

    @GetMapping("/benchmark")
    @Operation(summary = "DriftGuard demo endpoint")
    public DetectionBenchmarkReport benchmark() {
        return service.benchmark();
    }

    @GetMapping("/benchmark/profiles")
    @Operation(summary = "DriftGuard demo endpoint")
    public List<DetectionBenchmarkReport> benchmarkProfiles() {
        return service.benchmarkProfiles();
    }

    @GetMapping("/scenarios")
    @Operation(summary = "DriftGuard demo endpoint")
    public List<DemoScenarioDescriptor> scenarios() {
        return service.scenarios();
    }

    @PostMapping("/run")
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoRunResult rerun() {
        return service.runLatencyDegradation();
    }

    @PostMapping("/run/{scenario}")
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoRunResult runScenario(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) DemoScenarioRequest request
    ) {
        return service.run(scenario, request);
    }

    @PostMapping("/live/{scenario}")
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoRunResult startLiveScenario(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) DemoScenarioRequest request
    ) {
        return service.startLive(scenario, request);
    }

    @PostMapping("/live/stop")
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoRunResult stopLiveScenario() {
        service.stopLive();
        return service.lastResult();
    }

    @GetMapping("/kafka")
    @Operation(summary = "DriftGuard demo endpoint")
    public KafkaDemoStatus kafkaStatus() {
        return kafkaDemoService.status();
    }

    @GetMapping("/kafka/operations")
    @Operation(summary = "DriftGuard demo endpoint")
    public KafkaOperationsSnapshot kafkaOperations() {
        return kafkaOperationsService.snapshot();
    }

    @PostMapping("/kafka/start/{scenario}")
    @Operation(summary = "DriftGuard demo endpoint")
    public KafkaDemoStatus startKafkaScenario(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) DemoScenarioRequest request
    ) {
        return kafkaDemoService.start(scenario, request);
    }

    @PostMapping("/kafka/replay")
    @Operation(summary = "DriftGuard demo endpoint")
    public KafkaDemoStatus replayKafkaScenario(@RequestBody(required = false) KafkaReplayRequest request) {
        return kafkaDemoService.replay(request);
    }

    @PostMapping("/kafka/stop")
    @Operation(summary = "DriftGuard demo endpoint")
    public KafkaDemoStatus stopKafkaScenario() {
        return kafkaDemoService.stop();
    }

    @GetMapping("/capabilities")
    @Operation(summary = "DriftGuard demo endpoint")
    public List<DemoCapabilityGroup> capabilities() {
        return capabilityService.capabilities();
    }

    @GetMapping("/tools")
    @Operation(summary = "DriftGuard demo endpoint")
    public List<ToolLink> tools() {
        return List.of(
                new ToolLink("kafka-ui", "Kafka UI", toolProperties.getKafkaUiUrl(), "English demo text."),
                new ToolLink("prometheus", "Prometheus", toolProperties.getPrometheusUrl(), "English demo text."),
                new ToolLink("grafana", "Grafana", toolProperties.getGrafanaUrl(), "English demo text."),
                new ToolLink("swagger", "Swagger", toolProperties.getSwaggerUrl(), "REST API demo-application.")
        );
    }

    @GetMapping("/configuration")
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoConfigurationView configuration() {
        return configurationService.current();
    }

    @PostMapping("/configuration/profile/{profile}")
    @Operation(summary = "DriftGuard demo endpoint")
    public DemoConfigurationView updateProfile(@PathVariable("profile") String profile) {
        try {
            return configurationService.updateProfile(DemoDetectorProfile.parse(profile));
        } catch (IllegalArgumentException exception) {
            throw new DriftGuardValidationException(DemoErrorReason.UNKNOWN_PROFILE, profile);
        }
    }

    @GetMapping("/help")
    @Operation(summary = "DriftGuard demo endpoint")
    public Map<String, String> help() {
        return Map.ofEntries(
                Map.entry("overview", "GET /api/demo"),
                Map.entry("events", "GET /api/demo/events"),
                Map.entry("quality", "GET /api/demo/quality"),
                Map.entry("storedEvents", "GET /api/demo/events/stored"),
                Map.entry("clearStoredEvents", "POST /api/demo/events/clear"),
                Map.entry("benchmarkProfiles", "GET /api/demo/benchmark/profiles"),
                Map.entry("scenarios", "GET /api/demo/scenarios"),
                Map.entry("rerun", "POST /api/demo/run"),
                Map.entry("runScenario", "POST /api/demo/run/{scenario}"),
                Map.entry("startLiveScenario", "POST /api/demo/live/{scenario}"),
                Map.entry("stopLiveScenario", "POST /api/demo/live/stop"),
                Map.entry("benchmark", "GET /api/demo/benchmark"),
                Map.entry("replayKafkaScenario", "POST /api/demo/kafka/replay"),
                Map.entry("kafkaStatus", "GET /api/demo/kafka"),
                Map.entry("kafkaOperations", "GET /api/demo/kafka/operations"),
                Map.entry("startKafkaScenario", "POST /api/demo/kafka/start/{scenario}"),
                Map.entry("stopKafkaScenario", "POST /api/demo/kafka/stop"),
                Map.entry("tools", "GET /api/demo/tools"),
                Map.entry("capabilities", "GET /api/demo/capabilities"),
                Map.entry("configuration", "GET /api/demo/configuration"),
                Map.entry("updateProfile", "POST /api/demo/configuration/profile/{profile}")
        );
    }
}


