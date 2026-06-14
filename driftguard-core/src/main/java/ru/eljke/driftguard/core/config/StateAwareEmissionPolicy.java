package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Allows an algorithm configuration to select an emission policy from its
 * persisted detector state.
 */
public interface StateAwareEmissionPolicy {
    EmissionPolicyConfig emissionPolicy(DetectorState state, EmissionPolicyConfig fallback);
}
