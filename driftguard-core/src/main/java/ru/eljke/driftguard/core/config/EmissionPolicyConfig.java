package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Duration;

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
        DriftGuardErrors.require(minConsecutiveSignals > 0, "minConsecutiveSignals must be positive");
        cooldown = DriftGuardErrors.requireNonNull(cooldown, "cooldown");
        DriftGuardErrors.require(!cooldown.isNegative(), "cooldown must not be negative");
    }
}
