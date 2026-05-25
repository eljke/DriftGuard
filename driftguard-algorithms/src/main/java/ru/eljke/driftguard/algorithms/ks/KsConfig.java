package ru.eljke.driftguard.algorithms.ks;

import lombok.Builder;
import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Configuration for the two-sample Kolmogorov-Smirnov detector.
 *
 * @param baselineWindowSize number of observations in the reference sample
 * @param currentWindowSize number of latest observations in the current sample
 * @param warningPValue maximum p-value that produces a warning event
 * @param criticalPValue maximum p-value that produces a critical event
 */
@Builder(toBuilder = true)
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


