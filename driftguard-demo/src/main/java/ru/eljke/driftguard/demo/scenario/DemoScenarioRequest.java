package ru.eljke.driftguard.demo.scenario;

/**
 * Synthetic scenario run parameters.
 *
 * @param samples number of MetricPoint objects to generate for one stream
 * @param baselineValue stable value before drift
 * @param driftValue value during step/drop/spike drift
 * @param noiseStdDev noise standard deviation
 * @param driftStartPercent drift start position as a percentage of stream length
 * @param spikeLengthPercent spike duration as a percentage of stream length
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
    private static final int MAX_SAMPLES = 2000;

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


