package ru.eljke.driftguard.algorithms.adaptive;

import lombok.Builder;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.config.DetectorConfig;

@Builder
public record AdaptivePageHinkleyConfig(
        int calibrationSamples,
        PageHinkleyProfileSelector selector,
        PageHinkleyConfig aggressive,
        PageHinkleyConfig balanced,
        PageHinkleyConfig conservative
) implements DetectorConfig {
    public static final String ALGORITHM = "adaptive-page-hinkley";

    public AdaptivePageHinkleyConfig {
        if (calibrationSamples < 8) {
            throw new IllegalArgumentException("calibrationSamples must be at least 8");
        }
        if (selector == null || aggressive == null || balanced == null || conservative == null) {
            throw new IllegalArgumentException("selector and profile configurations are required");
        }
    }

    public PageHinkleyConfig profile(DetectorSensitivityProfile profile) {
        return switch (profile) {
            case AGGRESSIVE -> aggressive;
            case BALANCED -> balanced;
            case CONSERVATIVE -> conservative;
        };
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
