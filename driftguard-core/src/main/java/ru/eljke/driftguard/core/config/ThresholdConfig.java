package ru.eljke.driftguard.core.config;

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
        if (!Double.isFinite(warning) || warning <= 0) {
            throw new IllegalArgumentException("warning threshold must be positive and finite");
        }
        if (!Double.isFinite(critical) || critical < warning) {
            throw new IllegalArgumentException("critical threshold must be finite and greater than or equal to warning");
        }
    }
}
