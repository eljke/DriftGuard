package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * English API documentation.
 */
public final class StableNoiseScenario implements MetricScenario {
    private final String name;
    private final ScenarioConfig config;
    private final double mean;
    private final double noiseStdDev;

    public StableNoiseScenario(String name, ScenarioConfig config, double mean, double noiseStdDev) {
        this.name = name == null || name.isBlank() ? "stable-noise" : name;
        this.config = config;
        this.mean = mean;
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
            double value = mean + random.nextGaussian() * noiseStdDev;
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


