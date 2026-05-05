package ru.eljke.driftguard.demo.scenario;

/**
 * Параметры запуска synthetic scenario.
 *
 * @param samples сколько MetricPoint сгенерировать на один stream
 * @param baselineValue стабильное значение до drift-а
 * @param driftValue значение во время step/drop/spike drift-а
 * @param noiseStdDev стандартное отклонение шума
 * @param driftStartPercent позиция начала drift-а в процентах длины потока
 * @param spikeLengthPercent длительность spike-а в процентах длины потока
 */
public record DemoScenarioRequest(
        Integer samples,
        Double baselineValue,
        Double driftValue,
        Double noiseStdDev,
        Double driftStartPercent,
        Double spikeLengthPercent
) {
    public static final int DEFAULT_SAMPLES = 0;
    private static final int MIN_SAMPLES = 80;
    private static final int MAX_SAMPLES = 600;

    public DemoScenarioRequest(Integer samples) {
        this(samples, null, null, null, null, null);
    }

    public int normalizedSamples(int fallback) {
        int value = samples == null || samples <= 0 ? fallback : samples;
        return Math.clamp(value, MIN_SAMPLES, MAX_SAMPLES);
    }

    public double valueOrDefault(Double value, double fallback, double min, double max) {
        if (value == null || !Double.isFinite(value)) {
            return fallback;
        }
        return Math.clamp(value, min, max);
    }

    public double percentOrDefault(Double value, double fallback) {
        return valueOrDefault(value, fallback, 5.0, 95.0);
    }
}
