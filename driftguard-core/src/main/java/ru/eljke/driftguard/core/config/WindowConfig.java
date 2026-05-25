package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Shared fixed-window settings.
 *
 * @param size maximum number of observations retained in the window
 * @param minSamples minimum number of observations required before detection is allowed
 */
public record WindowConfig(
        int size,
        int minSamples
) {
    public WindowConfig {
        DriftGuardErrors.require(size > 0, "window size must be positive");
        DriftGuardErrors.require(minSamples > 0 && minSamples <= size, "minSamples must be in range [1, size]");
    }
}

