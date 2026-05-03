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
                "Сценарии, live playback, detector lifecycle и сохранённые drift events.",
                List.of(
                        ready(
                                "synthetic-scenarios",
                                "Synthetic scenarios",
                                "Запуск предсказуемых потоков latency, error-rate, throughput и queue-size.",
                                "engine",
                                List.of("GET /api/demo/scenarios", "POST /api/demo/run/{scenario}"),
                                List.of("Synthetic", "Overview")
                        ),
                        ready(
                                "live-playback",
                                "Live playback",
                                "Потоковая обработка точек без Kafka для быстрой проверки detector profile.",
                                "engine",
                                List.of("POST /api/demo/live/{scenario}", "POST /api/demo/live/stop"),
                                List.of("Synthetic")
                        ),
                        partial(
                                "event-lifecycle",
                                "Event lifecycle",
                                "UI показывает STARTED/ONGOING/RECOVERED, но сценарии recovery ещё стоит расширить.",
                                "engine",
                                List.of("GET /api/demo/events", "GET /api/demo/events/stored"),
                                List.of("Kafka Demo", "Synthetic", "Overview")
                        ),
                        ready(
                                "stored-events",
                                "Stored drift events",
                                "Общий recent-stream событий из synthetic, live и Kafka demo.",
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
                "Kafka replay, stateful processing, runtime status и operational telemetry.",
                List.of(
                        ready(
                                "kafka-scenario-replay",
                                "Kafka scenario replay",
                                "Переигрывание synthetic scenario через Kafka с выбором скорости и detector profile.",
                                "kafka",
                                List.of("POST /api/demo/kafka/start/{scenario}", "POST /api/demo/kafka/replay", "POST /api/demo/kafka/stop"),
                                List.of("Kafka Demo")
                        ),
                        partial(
                                "stateful-kafka-processing",
                                "Stateful Kafka processing",
                                "Runtime state store подключён, но отдельный restart/recovery экран ещё не выделен.",
                                "kafka",
                                List.of("GET /api/demo/kafka", "GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        ),
                        ready(
                                "kafka-operations-telemetry",
                                "Operations telemetry",
                                "Панель показывает processed/events/errors/latency и параметры topology.",
                                "kafka",
                                List.of("GET /api/demo/kafka/operations"),
                                List.of("Kafka Demo")
                        ),
                        planned(
                                "kafka-error-records",
                                "Kafka error records",
                                "Счётчики ошибок есть, но просмотр payload-ов error topic ещё не выведен в UI.",
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
                "Benchmark-и, runtime profiles и сравнение чувствительности detector-ов.",
                List.of(
                        ready(
                                "benchmark",
                                "Benchmark report",
                                "Precision, recall, false positives, missed intervals и latency-to-detect.",
                                "quality",
                                List.of("GET /api/demo/benchmark"),
                                List.of("Synthetic")
                        ),
                        ready(
                                "profile-comparison",
                                "Profile comparison",
                                "Сравнение conservative/balanced/aggressive профилей на одинаковых сценариях.",
                                "quality",
                                List.of("GET /api/demo/benchmark/profiles", "POST /api/demo/configuration/profile/{profile}"),
                                List.of("Synthetic", "Configuration")
                        ),
                        partial(
                                "quality-gates",
                                "Quality gates",
                                "Testkit поддерживает gates, но UI пока показывает только benchmark summary.",
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
                "Конфигурация, OpenAPI, Kafka UI, Actuator и вспомогательные инструменты demo-стенда.",
                List.of(
                        ready(
                                "configuration-view",
                                "Configuration view",
                                "Показывает detector definitions, Kafka properties и зарегистрированные алгоритмы.",
                                "configuration",
                                List.of("GET /api/demo/configuration"),
                                List.of("Configuration")
                        ),
                        ready(
                                "tool-links",
                                "Tool links",
                                "Быстрый переход к Swagger, Actuator, Kafka UI и другим локальным инструментам.",
                                "tools",
                                List.of("GET /api/demo/tools"),
                                List.of("Tools")
                        ),
                        ready(
                                "capability-map",
                                "Capability map",
                                "Показывает, какие возможности backend уже раскрыты в UI, а где остаются gaps.",
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
