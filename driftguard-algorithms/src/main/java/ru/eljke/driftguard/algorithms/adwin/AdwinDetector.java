package ru.eljke.driftguard.algorithms.adwin;

import ru.eljke.driftguard.algorithms.support.DriftEvents;
import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Arrays;
import java.util.Map;

/**
 * Detector с адаптивным окном для обнаружения сдвига среднего через Hoeffding-style bound.
 */
public final class AdwinDetector implements DetectorAlgorithm<AdwinConfig, AdwinState> {
    @Override
    public String name() {
        return AdwinConfig.ALGORITHM;
    }

    @Override
    public Class<AdwinConfig> configType() {
        return AdwinConfig.class;
    }

    @Override
    public AdwinState initialState(AdwinConfig config) {
        return new AdwinState(new SlidingDoubleWindow(config.windowSize()));
    }

    @Override
    public DetectionResult<AdwinState> detect(MetricPoint point, AdwinState state, AdwinConfig config, DetectionContext context) {
        SlidingDoubleWindow window = state.window().add(point.value());
        AdwinState next = new AdwinState(window);
        if (!window.isFull()) {
            return DetectionResult.noDrift(next);
        }
        double[] values = window.toArray();
        int split = values.length / 2;
        double[] left = Arrays.copyOfRange(values, 0, split);
        double[] right = Arrays.copyOfRange(values, split, values.length);
        double leftMean = Arrays.stream(left).average().orElse(0.0);
        double rightMean = Arrays.stream(right).average().orElse(0.0);
        double diff = Math.abs(rightMean - leftMean);
        double epsilon = Math.sqrt(0.5 * Math.log(4.0 / config.delta()) * (1.0 / left.length + 1.0 / right.length));
        if (left.length < config.minSubWindowSize() || right.length < config.minSubWindowSize() || diff <= epsilon) {
            return DetectionResult.noDrift(next);
        }
        DriftDirection direction = rightMean > leftMean ? DriftDirection.UP : DriftDirection.DOWN;
        DriftSeverity severity = diff >= epsilon * config.criticalMultiplier() ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                direction,
                severity,
                diff / epsilon,
                rightMean,
                leftMean,
                "ADWIN-style adaptive window mean difference exceeded Hoeffding bound",
                Map.of("epsilon", epsilon, "leftMean", leftMean, "rightMean", rightMean)
        ));
    }
}
