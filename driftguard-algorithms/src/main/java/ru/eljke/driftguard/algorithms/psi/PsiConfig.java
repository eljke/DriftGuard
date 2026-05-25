package ru.eljke.driftguard.algorithms.psi;

import lombok.Builder;
import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Configuration for Population Stability Index distribution drift detection.
 *
 * @param baselineWindowSize number of observations in the reference distribution
 * @param currentWindowSize number of latest observations compared with the baseline
 * @param buckets documented value
 * @param warningThreshold PSI value for warning events
 * @param criticalThreshold PSI value for critical events
 * @param epsilon minimum bucket share used to avoid division by zero
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


