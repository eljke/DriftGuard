package ru.eljke.driftguard.demo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eljke.driftguard.core.domain.DriftEvent;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo", description = "REST API демонстрационного сценария DriftGuard")
public class DemoController {
    private final DemoScenarioService service;

    public DemoController(DemoScenarioService service) {
        this.service = service;
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

    @GetMapping("/help")
    @Operation(summary = "Возвращает краткий список доступных demo endpoint-ов")
    public Map<String, String> help() {
        return Map.of(
                "overview", "GET /api/demo",
                "events", "GET /api/demo/events",
                "quality", "GET /api/demo/quality",
                "scenarios", "GET /api/demo/scenarios",
                "rerun", "POST /api/demo/run",
                "runScenario", "POST /api/demo/run/{scenario}",
                "startLiveScenario", "POST /api/demo/live/{scenario}",
                "stopLiveScenario", "POST /api/demo/live/stop"
        );
    }
}
