package ru.eljke.driftguard.testkit;

import java.time.Duration;

/**
 * Готовые профили quality-gates для тестов и CI.
 *
 * <p>Профили намеренно консервативные: они задают стартовые политики, которые
 * можно переопределить точечным gate-ом под конкретный алгоритм или сценарий.</p>
 */
public final class DetectionQualityGates {
    private DetectionQualityGates() {
    }

    /**
     * Мягкий gate для smoke-тестов и ранней разработки detector-а.
     */
    public static DetectionQualityGate smoke() {
        return DetectionQualityGate.builder()
                .minRecall(0.5)
                .maxMissedDriftIntervals(1)
                .build();
    }

    /**
     * Баланс между чувствительностью и шумом для обычного CI-прогона.
     */
    public static DetectionQualityGate balanced() {
        return DetectionQualityGate.builder()
                .minPrecision(0.75)
                .minRecall(0.75)
                .maxFalsePositiveEvents(3)
                .maxMissedDriftIntervals(1)
                .maxMeanFirstDetectionDelay(Duration.ofMinutes(2))
                .build();
    }

    /**
     * Строгий gate для стабильных detector-ов и regression-suite.
     */
    public static DetectionQualityGate strict() {
        return DetectionQualityGate.builder()
                .minPrecision(0.9)
                .minRecall(0.9)
                .maxFalsePositiveEvents(1)
                .maxMissedDriftIntervals(0)
                .maxMeanFirstDetectionDelay(Duration.ofSeconds(45))
                .build();
    }
}
