package ru.eljke.driftguard.testkit;

import java.util.List;

/**
 * Итог benchmark-прогона набора synthetic scenarios.
 *
 * @param label человекочитаемая метка benchmark-а, например профиль detector-ов
 * @param results результаты по отдельным сценариям
 * @param summary агрегированные метрики качества
 */
public record DetectionBenchmarkReport(
        String label,
        List<DetectionBenchmarkResult> results,
        DetectionBenchmarkSummary summary
) {
    public DetectionBenchmarkReport {
        label = label == null || label.isBlank() ? "default" : label;
        results = List.copyOf(results == null ? List.of() : results);
        summary = summary == null ? DetectionBenchmarkSummary.empty() : summary;
    }
}
