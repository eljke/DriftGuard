package ru.eljke.driftguard.testkit;

import java.util.Objects;

/**
 * Один сценарий внутри benchmark-suite.
 *
 * @param id стабильный идентификатор сценария для отчётов и CI-логов
 * @param scenario synthetic scenario, который генерирует поток метрик и ожидаемые drift-интервалы
 */
public record DetectionBenchmarkCase(
        String id,
        MetricScenario scenario
) {
    public DetectionBenchmarkCase {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        scenario = Objects.requireNonNull(scenario, "scenario must not be null");
    }

    public static DetectionBenchmarkCase of(String id, MetricScenario scenario) {
        return new DetectionBenchmarkCase(id, scenario);
    }
}
