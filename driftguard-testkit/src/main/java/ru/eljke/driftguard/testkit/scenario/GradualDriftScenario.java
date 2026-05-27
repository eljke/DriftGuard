package ru.eljke.driftguard.testkit.scenario;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Scenario where the metric value changes gradually after the drift point.
 */
public final class GradualDriftScenario implements MetricScenario {
    private final String name;
    private final ScenarioConfig config;
    private final int driftStartIndex;
    private final double startValue;
    private final double slope;
    private final double noiseStdDev;

    public GradualDriftScenario(String name, ScenarioConfig config, int driftStartIndex, double startValue, double slope, double noiseStdDev) {
        if (driftStartIndex <= 0 || driftStartIndex >= config.samples()) {
            throw new IllegalArgumentException("driftStartIndex must be inside generated sample range");
        }
        this.name = name == null || name.isBlank() ? "gradual-drift" : name;
        this.config = config;
        this.driftStartIndex = driftStartIndex;
        this.startValue = startValue;
        this.slope = slope;
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
            double trend = i < driftStartIndex ? 0.0 : (i - driftStartIndex) * slope;
            double value = startValue + trend + random.nextGaussian() * noiseStdDev;
            Instant timestamp = config.start().plus(config.step().multipliedBy(i));
            points.add(new MetricPoint(config.key(), timestamp, Math.max(0.0, value), config.kind(), Map.of("scenario", name), Map.of("sample", i)));
        }
        return List.copyOf(points);
    }

    @Override
    public List<DriftInterval> expectedDrifts() {
        Instant start = config.start().plus(config.step().multipliedBy(driftStartIndex));
        Instant end = config.start().plus(config.step().multipliedBy(config.samples() - 1L));
        return List.of(new DriftInterval(start, end));
    }
}


