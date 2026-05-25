package ru.eljke.driftguard.algorithms.chisquare;

import lombok.Builder;
import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Конфигурация binned хи-квадрат detector-а для drift-а распределения.
 *
 * @param baselineWindowSize число наблюдений в expected-распределении
 * @param currentWindowSize число последних наблюдений в observed-распределении
 * @param buckets число equal-width bucket-ов
 * @param warningPValue максимальный p-value, при котором создаётся warning event
 * @param criticalPValue максимальный p-value, при котором создаётся critical event
 * @param minExpectedCount bucket-ы с меньшим expected count игнорируются
 */
@Builder(toBuilder = true)
public record ChiSquareConfig(
        int baselineWindowSize,
        int currentWindowSize,
        int buckets,
        double warningPValue,
        double criticalPValue,
        double minExpectedCount
) implements DetectorConfig {
    public static final String ALGORITHM = "chi-square";

    public ChiSquareConfig {
        if (baselineWindowSize < 5 || currentWindowSize < 5) {
            throw new IllegalArgumentException("window sizes must be at least 5");
        }
        if (buckets < 2) {
            throw new IllegalArgumentException("buckets must be at least 2");
        }
        if (warningPValue <= 0 || warningPValue >= 1 || criticalPValue <= 0 || criticalPValue > warningPValue) {
            throw new IllegalArgumentException("p-value thresholds must satisfy 0 < critical <= warning < 1");
        }
        if (minExpectedCount <= 0) {
            throw new IllegalArgumentException("minExpectedCount must be positive");
        }
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
