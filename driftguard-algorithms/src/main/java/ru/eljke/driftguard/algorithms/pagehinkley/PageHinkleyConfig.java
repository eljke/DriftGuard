package ru.eljke.driftguard.algorithms.pagehinkley;

import ru.eljke.driftguard.core.config.DetectorConfig;

/**
 * Конфигурация онлайн detector-а Page-Hinkley для обнаружения сдвига среднего.
 *
 * @param warmupSamples число начальных точек, после которых разрешена генерация событий
 * @param delta допустимое малое изменение, вычитаемое из кумулятивной статистики
 * @param warningThreshold порог кумулятивной статистики для warning events
 * @param criticalThreshold порог кумулятивной статистики для critical events
 * @param alpha коэффициент обновления среднего; меньшие значения делают baseline медленнее
 */
public record PageHinkleyConfig(
        int warmupSamples,
        double delta,
        double warningThreshold,
        double criticalThreshold,
        double alpha
) implements DetectorConfig {
    public static final String ALGORITHM = "page-hinkley";

    public PageHinkleyConfig {
        if (warmupSamples < 2) {
            throw new IllegalArgumentException("warmupSamples must be at least 2");
        }
        if (delta < 0 || !Double.isFinite(delta)) {
            throw new IllegalArgumentException("delta must be finite and non-negative");
        }
        if (warningThreshold <= 0 || criticalThreshold < warningThreshold) {
            throw new IllegalArgumentException("thresholds must be positive and ordered");
        }
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in range (0, 1]");
        }
    }

    @Override
    public String algorithm() {
        return ALGORITHM;
    }
}
