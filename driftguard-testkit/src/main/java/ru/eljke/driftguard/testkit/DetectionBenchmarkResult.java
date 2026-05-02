package ru.eljke.driftguard.testkit;

/**
 * Результат benchmark-а для одного synthetic scenario.
 *
 * @param scenario технический id или имя сценария
 * @param metrics метрики качества детекции
 */
public record DetectionBenchmarkResult(
        String scenario,
        DetectionMetrics metrics
) {
}
