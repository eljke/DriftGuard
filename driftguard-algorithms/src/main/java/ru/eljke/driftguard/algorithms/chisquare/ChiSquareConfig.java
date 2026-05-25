package ru.eljke.driftguard.algorithms.chisquare;

import lombok.Builder;
import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Configuration for the binned chi-square distribution drift detector.
 *
 * @param baselineWindowSize number of observations in the expected distribution
 * @param currentWindowSize number of latest observations in the observed distribution
 * @param buckets number of equal-width buckets
 * @param warningPValue maximum p-value that produces a warning event
 * @param criticalPValue maximum p-value that produces a critical event
 * @param minExpectedCount buckets with lower expected count are ignored
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


