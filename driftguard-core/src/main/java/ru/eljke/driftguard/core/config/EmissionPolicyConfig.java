package ru.eljke.driftguard.core.config;

import lombok.Builder;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Duration;

/**
 * Policy for generating public drift events from internal algorithm signals.
 *
 * @param minConsecutiveSignals number of consecutive signals required before an event
 * @param cooldown minimum pause between events of one detector instance
 * @param recoveryConsecutiveNormal number of consecutive normal points required to close the current drift episode
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

    @Builder(builderMethodName = "builder")
    public static EmissionPolicyConfig of(
            Integer minConsecutiveSignals,
            Duration cooldown,
            Integer recoveryConsecutiveNormal
    ) {
        return new EmissionPolicyConfig(
                minConsecutiveSignals == null ? 1 : minConsecutiveSignals,
                cooldown == null ? Duration.ZERO : cooldown,
                recoveryConsecutiveNormal == null ? 1 : recoveryConsecutiveNormal
        );
    }
}

