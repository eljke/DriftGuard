package ru.eljke.driftguard.core.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Политика генерации публичных drift events поверх внутренних сигналов алгоритма.
 *
 * @param minConsecutiveSignals сколько подряд сигналов нужно получить перед событием
 * @param cooldown минимальная пауза между событиями одного detector instance
 */
public record EmissionPolicyConfig(
        int minConsecutiveSignals,
        Duration cooldown
) {
    public static final EmissionPolicyConfig DEFAULT = new EmissionPolicyConfig(1, Duration.ZERO);

    public EmissionPolicyConfig {
        if (minConsecutiveSignals <= 0) {
            throw new IllegalArgumentException("minConsecutiveSignals must be positive");
        }
        cooldown = Objects.requireNonNull(cooldown, "cooldown must not be null");
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("cooldown must not be negative");
        }
    }
}
