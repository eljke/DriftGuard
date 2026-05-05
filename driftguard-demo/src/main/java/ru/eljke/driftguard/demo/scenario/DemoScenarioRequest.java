package ru.eljke.driftguard.demo.scenario;

/**
 * Параметры запуска synthetic scenario.
 *
 * @param samples сколько MetricPoint сгенерировать на один stream
 */
public record DemoScenarioRequest(Integer samples) {
    public static final int DEFAULT_SAMPLES = 0;
    private static final int MIN_SAMPLES = 80;
    private static final int MAX_SAMPLES = 600;

    public int normalizedSamples(int fallback) {
        int value = samples == null || samples <= 0 ? fallback : samples;
        return Math.clamp(value, MIN_SAMPLES, MAX_SAMPLES);
    }
}
