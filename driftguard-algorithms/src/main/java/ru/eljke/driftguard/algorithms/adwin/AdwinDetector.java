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
        if (window.size() < config.minSubWindowSize() * 2) {
            return DetectionResult.noDrift(new AdwinState(window));
        }
        Cut best = findBestCut(window.toArray(), config);
        if (best == null) {
            return DetectionResult.noDrift(new AdwinState(window));
        }
        double[] currentValues = Arrays.copyOfRange(window.toArray(), best.split(), window.size());
        AdwinState next = new AdwinState(SlidingDoubleWindow.of(config.windowSize(), currentValues));
        DriftDirection direction = best.rightMean() > best.leftMean() ? DriftDirection.UP : DriftDirection.DOWN;
        DriftSeverity severity = best.score() >= config.criticalMultiplier() ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                direction,
                severity,
                best.score(),
                best.rightMean(),
                best.leftMean(),
                "ADWIN-style adaptive window mean difference exceeded Hoeffding bound",
                DriftEvents.standardDetails(
                        best.leftMean(),
                        best.rightMean(),
                        1.0,
                        config.criticalMultiplier(),
                        Map.of(
                                "epsilon", best.epsilon(),
                                "split", best.split(),
                                "windowSize", window.size(),
                                "meanDifference", Math.abs(best.rightMean() - best.leftMean()),
                                "scoreMultiplier", best.score()
                        )
                )
        ));
    }

    private Cut findBestCut(double[] values, AdwinConfig config) {
        Cut best = null;
        for (int split = config.minSubWindowSize(); split <= values.length - config.minSubWindowSize(); split++) {
            Cut candidate = evaluate(values, split, config);
            if (candidate != null && (best == null || candidate.score() > best.score())) {
                best = candidate;
            }
        }
        return best;
    }

    private Cut evaluate(double[] values, int split, AdwinConfig config) {
        double[] left = Arrays.copyOfRange(values, 0, split);
        double[] right = Arrays.copyOfRange(values, split, values.length);
        double leftMean = Arrays.stream(left).average().orElse(0.0);
        double rightMean = Arrays.stream(right).average().orElse(0.0);
        double diff = Math.abs(rightMean - leftMean);
        double epsilon = Math.sqrt(0.5 * Math.log(4.0 / config.delta()) * (1.0 / left.length + 1.0 / right.length));
        if (diff <= epsilon) {
            return null;
        }
        return new Cut(split, leftMean, rightMean, epsilon, diff / epsilon);
    }

    private record Cut(int split, double leftMean, double rightMean, double epsilon, double score) {
        private Cut {
            if (split <= 0) {
                throw new IllegalArgumentException("split must be positive");
            }
        }
    }
}
