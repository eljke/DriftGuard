package ru.eljke.driftguard.algorithms.ks;

import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Конфигурация двухвыборочного detector-а Колмогорова-Смирнова.
 *
 * @param baselineWindowSize число наблюдений в reference-выборке
 * @param currentWindowSize число последних наблюдений в current-выборке
 * @param warningPValue максимальный p-value, при котором создаётся warning event
 * @param criticalPValue максимальный p-value, при котором создаётся critical event
 */
public record KsConfig(
        int baselineWindowSize,
        int currentWindowSize,
        double warningPValue,
        double criticalPValue
) implements DetectorConfig {
    public static final String ALGORITHM = "ks";

    public KsConfig {
        if (baselineWindowSize < 2 || currentWindowSize < 2) {
            throw new IllegalArgumentException("window sizes must be at least 2");
        }
        if (warningPValue <= 0 || warningPValue >= 1 || criticalPValue <= 0 || criticalPValue > warningPValue) {
            throw new IllegalArgumentException("p-value thresholds must satisfy 0 < critical <= warning < 1");
        }
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
