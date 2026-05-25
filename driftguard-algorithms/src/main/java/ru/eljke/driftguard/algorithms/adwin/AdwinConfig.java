package ru.eljke.driftguard.algorithms.adwin;

import lombok.Builder;
import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Configuration for the ADWIN adaptive-window mean-shift detector.
 *
 * <p>The implementation keeps an exact bounded window and applies the ADWIN
 * cut test with a variance-aware confidence bound. When a cut is accepted, the
 * older sub-window is discarded and the detector continues with the recent
 * observations.</p>
 *
 * @param windowSize maximum retained adaptive window size
 * @param minSubWindowSize minimum size of each candidate sub-window
 * @param delta confidence parameter used by the ADWIN cut bound
 * @param criticalMultiplier score multiplier that promotes an event to critical
 */
@Builder(toBuilder = true)
public record AdwinConfig(
        int windowSize,
        int minSubWindowSize,
        double delta,
        double criticalMultiplier
) implements DetectorConfig {
    public static final String ALGORITHM = "adwin";

    public AdwinConfig {
        if (windowSize < 4) {
            throw new IllegalArgumentException("windowSize must be at least 4");
        }
        if (minSubWindowSize < 2 || minSubWindowSize * 2 > windowSize) {
            throw new IllegalArgumentException("minSubWindowSize must allow two sub windows");
        }
        if (delta <= 0 || delta >= 1) {
            throw new IllegalArgumentException("delta must be in range (0, 1)");
        }
        if (criticalMultiplier < 1) {
            throw new IllegalArgumentException("criticalMultiplier must be at least 1");
        }
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
