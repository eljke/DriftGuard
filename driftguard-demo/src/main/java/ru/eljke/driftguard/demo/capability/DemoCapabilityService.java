package ru.eljke.driftguard.demo.capability;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoCapabilityService {
    public List<DemoCapabilityGroup> capabilities() {
        return List.of(
                detectionEngine(),
                kafkaOperations(),
                qualityAndProfiles(),
                observabilityAndTooling()
        );
    }

    private DemoCapabilityGroup detectionEngine() {
        return new DemoCapabilityGroup(
                "detection-engine",
                "Detection engine",
                "English demo text.",
                List.of(
                        ready(
                                "synthetic-scenarios",
                                "Synthetic scenarios",
                                "English demo text.",
                                "engine",
                                List.of("GET /api/demo/scenarios", "POST /api/demo/run/{scenario}"),
                                List.of("Synthetic", "Overview")
                        ),
                        ready(
                                "live-playback",
                                "Live playback",
                                "English demo text.",
                                "engine",
                                List.of("POST /api/demo/live/{scenario}", "POST /api/demo/live/stop"),
                                List.of("Synthetic")
                        ),
                        partial(
                                "event-lifecycle",
                                "Event lifecycle",
                                "English demo text.",
                                "engine",
                                List.of("GET /api/demo/events", "GET /api/demo/events/stored"),
                                List.of("Kafka Demo", "Synthetic", "Overview")
                        ),
                        ready(
                                "stored-events",
                                "Stored drift events",
                                "English demo text.",
                                "engine",
                                List.of("GET /api/demo/events/stored", "POST /api/demo/events/clear"),
                                List.of("Overview")
                        )
                )
        );
    }

    private DemoCapabilityGroup kafkaOperations() {
        return new DemoCapabilityGroup(
                "kafka-operations",
                "Kafka operations",
                "English demo text.",
                List.of(
                        ready(
                                "kafka-scenario-replay",
                                "Kafka scenario replay",
                                "English demo text.",
                                "kafka",
                                List.of("POST /api/demo/kafka/start/{scenario}", "POST /api/demo/kafka/replay", "POST /api/demo/kafka/stop"),
                                List.of("Kafka Demo")
                        ),
                        partial(
                                "stateful-kafka-processing",
                                "Stateful Kafka processing",
                                "English demo text.",
                                "kafka",
                                List.of("GET /api/demo/kafka", "GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        ),
                        ready(
                                "kafka-operations-telemetry",
                                "Operations telemetry",
                                "English demo text.",
                                "kafka",
                                List.of("GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        ),
                        planned(
                                "kafka-error-records",
                                "Kafka error records",
                                "English demo text.",
                                "kafka",
                                List.of("GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        )
                )
        );
    }

    private DemoCapabilityGroup qualityAndProfiles() {
        return new DemoCapabilityGroup(
                "quality-and-profiles",
                "Quality and profiles",
                "English demo text.",
                List.of(
                        ready(
                                "benchmark",
                                "Benchmark report",
                                "English demo text.",
                                "quality",
                                List.of("GET /api/demo/benchmark"),
                                List.of("Synthetic")
                        ),
                        ready(
                                "profile-comparison",
                                "Profile comparison",
                                "English demo text.",
                                "quality",
                                List.of("GET /api/demo/benchmark/profiles", "POST /api/demo/configuration/profile/{profile}"),
                                List.of("Synthetic", "Configuration")
                        ),
                        partial(
                                "quality-gates",
                                "Quality gates",
                                "English demo text.",
                                "quality",
                                List.of("GET /api/demo/benchmark", "GET /api/demo/benchmark/profiles"),
                                List.of("Synthetic")
                        )
                )
        );
    }

    private DemoCapabilityGroup observabilityAndTooling() {
        return new DemoCapabilityGroup(
                "observability-and-tooling",
                "Observability and tooling",
                "English demo text.",
                List.of(
                        ready(
                                "configuration-view",
                                "Configuration view",
                                "English demo text.",
                                "configuration",
                                List.of("GET /api/demo/configuration"),
                                List.of("Configuration")
                        ),
                        ready(
                                "tool-links",
                                "Tool links",
                                "English demo text.",
                                "tools",
                                List.of("GET /api/demo/tools"),
                                List.of("Tools")
                        ),
                        ready(
                                "capability-map",
                                "Capability map",
                                "English demo text.",
                                "tools",
                                List.of("GET /api/demo/capabilities"),
                                List.of("Overview")
                        )
                )
        );
    }

    private static DemoCapability ready(
            String id,
            String title,
            String description,
            String category,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return capability(id, title, description, category, DemoCapabilityStatus.READY, apiEndpoints, uiSurfaces);
    }

    private static DemoCapability partial(
            String id,
            String title,
            String description,
            String category,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return capability(id, title, description, category, DemoCapabilityStatus.PARTIAL, apiEndpoints, uiSurfaces);
    }

    @SuppressWarnings("SameParameterValue")
    private static DemoCapability planned(
            String id,
            String title,
            String description,
            String category,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return capability(id, title, description, category, DemoCapabilityStatus.PLANNED, apiEndpoints, uiSurfaces);
    }

    private static DemoCapability capability(
            String id,
            String title,
            String description,
            String category,
            DemoCapabilityStatus status,
            List<String> apiEndpoints,
            List<String> uiSurfaces
    ) {
        return new DemoCapability(
                id,
                title,
                description,
                category,
                status,
                apiEndpoints,
                uiSurfaces
        );
    }
}


