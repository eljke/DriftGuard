package ru.eljke.driftguard.testkit.scenario;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Scenario with an abrupt persistent throughput decrease.
 */
public final class ThroughputDropScenario implements MetricScenario {
    private final String name;
    private final ScenarioConfig config;
    private final int dropStartIndex;
    private final double beforeMean;
    private final double afterMean;
    private final double noiseStdDev;

    public ThroughputDropScenario(String name, ScenarioConfig config, int dropStartIndex, double beforeMean, double afterMean, double noiseStdDev) {
        if (dropStartIndex <= 0 || dropStartIndex >= config.samples()) {
            throw new IllegalArgumentException("dropStartIndex must be inside generated sample range");
        }
        if (afterMean >= beforeMean) {
            throw new IllegalArgumentException("afterMean must be lower than beforeMean");
        }
        this.name = name == null || name.isBlank() ? "throughput-drop" : name;
        this.config = config;
        this.dropStartIndex = dropStartIndex;
        this.beforeMean = beforeMean;
        this.afterMean = afterMean;
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
            double mean = i < dropStartIndex ? beforeMean : afterMean;
            double value = mean + random.nextGaussian() * noiseStdDev;
            Instant timestamp = config.start().plus(config.step().multipliedBy(i));
            points.add(new MetricPoint(config.key(), timestamp, Math.max(0.0, value), config.kind(), Map.of("scenario", name), Map.of("sample", i)));
        }
        return List.copyOf(points);
    }

    @Override
    public List<DriftInterval> expectedDrifts() {
        Instant start = config.start().plus(config.step().multipliedBy(dropStartIndex));
        Instant end = config.start().plus(config.step().multipliedBy(config.samples() - 1L));
        return List.of(new DriftInterval(start, end));
    }
}


