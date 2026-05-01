package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Общая пара порогов warning/critical для алгоритмов со скалярным score.
 *
 * @param warning score, начиная с которого drift event получает уровень warning
 * @param critical score, начиная с которого drift event получает уровень critical
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
