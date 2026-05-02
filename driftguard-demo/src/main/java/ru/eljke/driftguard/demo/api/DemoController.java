package ru.eljke.driftguard.demo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.demo.config.DemoToolProperties;
import ru.eljke.driftguard.demo.kafka.KafkaDemoService;
import ru.eljke.driftguard.demo.kafka.KafkaDemoStatus;
import ru.eljke.driftguard.demo.scenario.DemoRunResult;
import ru.eljke.driftguard.demo.scenario.DemoScenarioDescriptor;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.demo.tool.ToolLink;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "REST API демонстрационного сценария DriftGuard")
public class DemoController {
    private final DemoScenarioService service;
    private final KafkaDemoService kafkaDemoService;
    private final DemoToolProperties toolProperties;

    public DemoController(DemoScenarioService service, KafkaDemoService kafkaDemoService, DemoToolProperties toolProperties) {
        this.service = service;
        this.kafkaDemoService = kafkaDemoService;
        this.toolProperties = toolProperties;
    }

    @GetMapping
    @Operation(summary = "Возвращает последний результат демонстрационного запуска")
    public DemoRunResult overview() {
        return service.lastResult();
    }

    @GetMapping("/events")
    @Operation(summary = "Возвращает события дрейфа из последнего запуска")
    public List<DriftEvent> events() {
        return service.lastResult().events();
    }

    @GetMapping("/quality")
    @Operation(summary = "Возвращает оценку качества детекции из последнего запуска")
    public Object quality() {
        return service.lastResult().quality();
    }

    @GetMapping("/scenarios")
    @Operation(summary = "Возвращает доступные демонстрационные сценарии")
    public List<DemoScenarioDescriptor> scenarios() {
        return service.scenarios();
    }

    @PostMapping("/run")
    @Operation(summary = "Перезапускает демонстрационный сценарий деградации latency")
    public DemoRunResult rerun() {
        return service.runLatencyDegradation();
    }

    @PostMapping("/run/{scenario}")
    @Operation(summary = "Запускает выбранный демонстрационный сценарий")
    public DemoRunResult runScenario(@PathVariable("scenario") String scenario) {
        return service.run(scenario);
    }

    @PostMapping("/live/{scenario}")
    @Operation(summary = "Запускает live playback выбранного сценария")
    public DemoRunResult startLiveScenario(@PathVariable("scenario") String scenario) {
        return service.startLive(scenario);
    }

    @PostMapping("/live/stop")
    @Operation(summary = "Останавливает live playback")
    public DemoRunResult stopLiveScenario() {
        service.stopLive();
        return service.lastResult();
    }

    @GetMapping("/kafka")
    @Operation(summary = "Возвращает состояние интеграционного Kafka demo")
    public KafkaDemoStatus kafkaStatus() {
        return kafkaDemoService.status();
    }

    @PostMapping("/kafka/start/{scenario}")
    @Operation(summary = "Запускает Kafka producer, Kafka Streams topology и consumer событий")
    public KafkaDemoStatus startKafkaScenario(@PathVariable("scenario") String scenario) {
        return kafkaDemoService.start(scenario);
    }

    @PostMapping("/kafka/stop")
    @Operation(summary = "Останавливает интеграционный Kafka demo")
    public KafkaDemoStatus stopKafkaScenario() {
        return kafkaDemoService.stop();
    }

    @GetMapping("/tools")
    @Operation(summary = "Возвращает ссылки на инструменты локального demo-стенда")
    public List<ToolLink> tools() {
        return List.of(
                new ToolLink("kafka-ui", "Kafka UI", toolProperties.getKafkaUiUrl(), "Topic-и, consumer groups и сообщения Kafka."),
                new ToolLink("prometheus", "Prometheus", toolProperties.getPrometheusUrl(), "Scrape target-ы и raw metrics DriftGuard."),
                new ToolLink("grafana", "Grafana", toolProperties.getGrafanaUrl(), "Dashboard для метрик DriftGuard."),
                new ToolLink("swagger", "Swagger", toolProperties.getSwaggerUrl(), "REST API demo-приложения.")
        );
    }

    @GetMapping("/help")
    @Operation(summary = "Возвращает краткий список доступных demo endpoint-ов")
    public Map<String, String> help() {
        return Map.ofEntries(
                Map.entry("overview", "GET /api/demo"),
                Map.entry("events", "GET /api/demo/events"),
                Map.entry("quality", "GET /api/demo/quality"),
                Map.entry("scenarios", "GET /api/demo/scenarios"),
                Map.entry("rerun", "POST /api/demo/run"),
                Map.entry("runScenario", "POST /api/demo/run/{scenario}"),
                Map.entry("startLiveScenario", "POST /api/demo/live/{scenario}"),
                Map.entry("stopLiveScenario", "POST /api/demo/live/stop"),
                Map.entry("kafkaStatus", "GET /api/demo/kafka"),
                Map.entry("startKafkaScenario", "POST /api/demo/kafka/start/{scenario}"),
                Map.entry("stopKafkaScenario", "POST /api/demo/kafka/stop"),
                Map.entry("tools", "GET /api/demo/tools")
        );
    }
}
