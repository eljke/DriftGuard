package ru.eljke.driftguard.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.eljke.driftguard.core.domain.DriftEvent;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoController {
    private final DemoScenarioService service;

    public DemoController(DemoScenarioService service) {
        this.service = service;
    }

    @GetMapping
    public DemoRunResult overview() {
        return service.lastResult();
    }

    @GetMapping("/events")
    public List<DriftEvent> events() {
        return service.lastResult().events();
    }

    @GetMapping("/quality")
    public Object quality() {
        return service.lastResult().quality();
    }

    @PostMapping("/run")
    public DemoRunResult rerun() {
        return service.runLatencyDegradation();
    }

    @GetMapping("/help")
    public Map<String, String> help() {
        return Map.of(
                "overview", "GET /api/demo",
                "events", "GET /api/demo/events",
                "quality", "GET /api/demo/quality",
                "rerun", "POST /api/demo/run"
        );
    }
}
