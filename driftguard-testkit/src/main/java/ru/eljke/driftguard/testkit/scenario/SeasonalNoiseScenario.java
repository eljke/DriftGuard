package ru.eljke.driftguard.testkit.scenario;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Stable seasonal stream without expected drift.
 *
 * <p>This scenario verifies that a detector does not treat a regular
 * Scenario with periodic variation that should not be treated as drift.
 */
public final class SeasonalNoiseScenario implements MetricScenario {
    private final String name;
    private final ScenarioConfig config;
    private final double mean;
    private final double amplitude;
    private final int periodSamples;
    private final double noiseStdDev;

    public SeasonalNoiseScenario(String name, ScenarioConfig config, double mean, double amplitude, int periodSamples, double noiseStdDev) {
        if (periodSamples <= 1) {
            throw new IllegalArgumentException("periodSamples must be greater than 1");
        }
        this.name = name == null || name.isBlank() ? "seasonal-noise" : name;
        this.config = config;
        this.mean = mean;
        this.amplitude = amplitude;
        this.periodSamples = periodSamples;
        this.noiseStdDev = noiseStdDev;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<MetricPoint> generate() {
        Random random = new Random(config.seed());
        List<MetricPoint> points = new ArrayList<>(config.samples());
        for (int i = 0; i < config.samples(); i++) {
            double phase = 2.0 * Math.PI * (i % periodSamples) / periodSamples;
            double value = mean + Math.sin(phase) * amplitude + random.nextGaussian() * noiseStdDev;
            Instant timestamp = config.start().plus(config.step().multipliedBy(i));
            points.add(new MetricPoint(config.key(), timestamp, Math.max(0.0, value), config.kind(), Map.of("scenario", name), Map.of("sample", i)));
        }
        return List.copyOf(points);
    }

    @Override
    public List<DriftInterval> expectedDrifts() {
        return List.of();
    }
}


