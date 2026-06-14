package ru.eljke.driftguard.algorithms.adaptive;

import lombok.Builder;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.config.StateAwareEmissionPolicy;
import ru.eljke.driftguard.core.detector.DetectorState;

@Builder
public record AdaptivePageHinkleyConfig(
        int calibrationSamples,
        PageHinkleyProfileSelector selector,
        PageHinkleyConfig aggressive,
        PageHinkleyConfig balanced,
        PageHinkleyConfig conservative,
        EmissionPolicyConfig aggressiveEmissionPolicy,
        EmissionPolicyConfig balancedEmissionPolicy,
        EmissionPolicyConfig conservativeEmissionPolicy
) implements DetectorConfig, StateAwareEmissionPolicy {
    public static final String ALGORITHM = "adaptive-page-hinkley";

    public AdaptivePageHinkleyConfig {
        if (calibrationSamples < 8) {
            throw new IllegalArgumentException("calibrationSamples must be at least 8");
        }
        if (selector == null || aggressive == null || balanced == null || conservative == null) {
            throw new IllegalArgumentException("selector and profile configurations are required");
        }
    }

    public AdaptivePageHinkleyConfig(
            int calibrationSamples,
            PageHinkleyProfileSelector selector,
            PageHinkleyConfig aggressive,
            PageHinkleyConfig balanced,
            PageHinkleyConfig conservative
    ) {
        this(calibrationSamples, selector, aggressive, balanced, conservative, null, null, null);
    }

    public PageHinkleyConfig profile(DetectorSensitivityProfile profile) {
        return switch (profile) {
            case AGGRESSIVE -> aggressive;
            case BALANCED -> balanced;
            case CONSERVATIVE -> conservative;
        };
    }

    @Override
    public EmissionPolicyConfig emissionPolicy(DetectorState state, EmissionPolicyConfig fallback) {
        AdaptivePageHinkleyState adaptiveState = (AdaptivePageHinkleyState) state;
        if (!adaptiveState.calibrated()) {
            return fallback;
        }
        EmissionPolicyConfig selected = switch (adaptiveState.selectedProfile()) {
            case AGGRESSIVE -> aggressiveEmissionPolicy;
            case BALANCED -> balancedEmissionPolicy;
            case CONSERVATIVE -> conservativeEmissionPolicy;
        };
        return selected == null ? fallback : selected;
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
