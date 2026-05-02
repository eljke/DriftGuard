package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Duration;

/**
 * Политика генерации публичных drift events поверх внутренних сигналов алгоритма.
 *
 * @param minConsecutiveSignals сколько подряд сигналов нужно получить перед событием
 * @param cooldown минимальная пауза между событиями одного detector instance
 * @param recoveryConsecutiveNormal сколько подряд нормальных точек нужно для закрытия текущего drift episode
 */
public record EmissionPolicyConfig(
        int minConsecutiveSignals,
        Duration cooldown,
        int recoveryConsecutiveNormal
) {
    public static final EmissionPolicyConfig DEFAULT = new EmissionPolicyConfig(1, Duration.ZERO, 1);

    public EmissionPolicyConfig(int minConsecutiveSignals, Duration cooldown) {
        this(minConsecutiveSignals, cooldown, 1);
    }

    public EmissionPolicyConfig {
        DriftGuardErrors.require(minConsecutiveSignals > 0, "minConsecutiveSignals must be positive");
        cooldown = DriftGuardErrors.requireNonNull(cooldown, "cooldown");
        DriftGuardErrors.require(!cooldown.isNegative(), "cooldown must not be negative");
        DriftGuardErrors.require(recoveryConsecutiveNormal > 0, "recoveryConsecutiveNormal must be positive");
    }
}
