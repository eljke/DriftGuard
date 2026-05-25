package ru.eljke.driftguard.algorithms.psi;

import lombok.Builder;
import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Конфигурация Population Stability Index для обнаружения drift-а распределения.
 *
 * @param baselineWindowSize число наблюдений в reference-распределении
 * @param currentWindowSize число последних наблюдений, сравниваемых с baseline
 * @param buckets число equal-width bucket-ов для сравнения распределений
 * @param warningThreshold значение PSI для warning events
 * @param criticalThreshold значение PSI для critical events
 * @param epsilon минимальная доля bucket-а, используемая для защиты от деления на ноль
 */
@Builder(toBuilder = true)
public record PsiConfig(
        int baselineWindowSize,
        int currentWindowSize,
        int buckets,
        double warningThreshold,
        double criticalThreshold,
        double epsilon
) implements DetectorConfig {
    public static final String ALGORITHM = "psi";

    public PsiConfig {
        if (baselineWindowSize < 2 || currentWindowSize < 2) {
            throw new IllegalArgumentException("window sizes must be at least 2");
        }
        if (buckets < 2) {
            throw new IllegalArgumentException("buckets must be at least 2");
        }
        if (warningThreshold <= 0 || criticalThreshold < warningThreshold) {
            throw new IllegalArgumentException("thresholds must be positive and ordered");
        }
        if (epsilon <= 0) {
            throw new IllegalArgumentException("epsilon must be positive");
        }
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
