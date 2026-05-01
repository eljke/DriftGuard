package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Общие настройки фиксированного окна.
 *
 * @param size максимальное число наблюдений, удерживаемых в окне
 * @param minSamples минимальное число наблюдений, после которого разрешена детекция
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
