package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Кратковременный spike метрики, например всплеск error rate после deploy-а.
 */
public final class PulseSpikeScenario implements MetricScenario {
    private final String name;
    private final ScenarioConfig config;
    private final int spikeStartIndex;
    private final int spikeLength;
    private final double baselineMean;
    private final double spikeMean;
    private final double noiseStdDev;

    public PulseSpikeScenario(
            String name,
            ScenarioConfig config,
            int spikeStartIndex,
            int spikeLength,
            double baselineMean,
            double spikeMean,
            double noiseStdDev
    ) {
        if (spikeStartIndex <= 0 || spikeStartIndex >= config.samples()) {
            throw new IllegalArgumentException("spikeStartIndex must be inside generated sample range");
        }
        if (spikeLength <= 0 || spikeStartIndex + spikeLength > config.samples()) {
            throw new IllegalArgumentException("spikeLength must fit generated sample range");
        }
        this.name = name == null || name.isBlank() ? "pulse-spike" : name;
        this.config = config;
        this.spikeStartIndex = spikeStartIndex;
        this.spikeLength = spikeLength;
        this.baselineMean = baselineMean;
        this.spikeMean = spikeMean;
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
        int spikeEndIndex = spikeStartIndex + spikeLength;
        for (int i = 0; i < config.samples(); i++) {
            double mean = i >= spikeStartIndex && i < spikeEndIndex ? spikeMean : baselineMean;
            double value = mean + random.nextGaussian() * noiseStdDev;
            Instant timestamp = config.start().plus(config.step().multipliedBy(i));
            points.add(new MetricPoint(config.key(), timestamp, Math.max(0.0, value), config.kind(), Map.of("scenario", name), Map.of("sample", i)));
        }
        return List.copyOf(points);
    }

    @Override
    public List<DriftInterval> expectedDrifts() {
        Instant start = config.start().plus(config.step().multipliedBy(spikeStartIndex));
        Instant end = config.start().plus(config.step().multipliedBy(spikeStartIndex + spikeLength - 1L));
        return List.of(new DriftInterval(start, end));
    }
}
