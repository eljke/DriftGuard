package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Сценарий резкой деградации: после заданного индекса baseline переходит на
 * новый устойчивый уровень.
 */
public final class StepDriftScenario implements MetricScenario {
    private final String name;
    private final ScenarioConfig config;
    private final int driftStartIndex;
    private final double beforeMean;
    private final double afterMean;
    private final double noiseStdDev;

    public StepDriftScenario(
            String name,
            ScenarioConfig config,
            int driftStartIndex,
            double beforeMean,
            double afterMean,
            double noiseStdDev
    ) {
        if (driftStartIndex <= 0 || driftStartIndex >= config.samples()) {
            throw new IllegalArgumentException("driftStartIndex must be inside generated sample range");
        }
        this.name = name == null || name.isBlank() ? "step-drift" : name;
        this.config = config;
        this.driftStartIndex = driftStartIndex;
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
            double mean = i < driftStartIndex ? beforeMean : afterMean;
            double value = mean + random.nextGaussian() * noiseStdDev;
            Instant timestamp = config.start().plus(config.step().multipliedBy(i));
            points.add(new MetricPoint(
                    config.key(),
                    timestamp,
                    Math.max(0.0, value),
                    config.kind(),
                    Map.of("scenario", name),
                    Map.of("sample", i)
            ));
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
