package ru.eljke.driftguard.algorithms.adwin;

import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Конфигурация ADWIN-style detector-а с адаптивным окном.
 *
 * <p>Detector проверяет несколько возможных разрезов окна через Hoeffding
 * bound. После подтверждённого drift-а старый фрагмент окна отбрасывается,
 * а новое состояние продолжает работать с актуальной частью потока.</p>
 *
 * @param windowSize общий размер sliding window
 * @param minSubWindowSize минимальный размер каждого сравниваемого sub-window
 * @param delta статистический confidence-параметр, используемый в bound
 * @param criticalMultiplier множитель score, после которого severity становится critical
 */
public record AdwinConfig(
        int windowSize,
        int minSubWindowSize,
        double delta,
        double criticalMultiplier
) implements DetectorConfig {
    public static final String ALGORITHM = "adwin";

    public AdwinConfig {
        if (windowSize < 4) {
            throw new IllegalArgumentException("windowSize must be at least 4");
        }
        if (minSubWindowSize < 2 || minSubWindowSize * 2 > windowSize) {
            throw new IllegalArgumentException("minSubWindowSize must allow two sub windows");
        }
        if (delta <= 0 || delta >= 1) {
            throw new IllegalArgumentException("delta must be in range (0, 1)");
        }
        if (criticalMultiplier < 1) {
            throw new IllegalArgumentException("criticalMultiplier must be at least 1");
        }
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
