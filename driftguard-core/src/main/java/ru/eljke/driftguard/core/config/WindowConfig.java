package ru.eljke.driftguard.core.config;

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
        if (size <= 0) {
            throw new IllegalArgumentException("window size must be positive");
        }
        if (minSamples <= 0 || minSamples > size) {
            throw new IllegalArgumentException("minSamples must be in range [1, size]");
        }
    }
}
