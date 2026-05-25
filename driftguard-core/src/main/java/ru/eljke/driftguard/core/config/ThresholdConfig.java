package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Shared warning/critical threshold pair for algorithms with a scalar score.
 *
 * @param warning score, score from which a drift event receives warning severity
 * @param critical score, score from which a drift event receives critical severity
 */
public record ThresholdConfig(
        double warning,
        double critical
) {
    public ThresholdConfig {
        DriftGuardErrors.require(Double.isFinite(warning) && warning > 0, "warning threshold must be positive and finite");
        DriftGuardErrors.require(Double.isFinite(critical) && critical >= warning, "critical threshold must be finite and greater than or equal to warning");
    }
}

