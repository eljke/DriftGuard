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
 * ADWIN detector for online mean-shift detection.
 *
 * <p>This is the exact-window variant of ADWIN: it scans candidate cuts in the
 * current adaptive window, applies the variance-aware ADWIN bound and drops
 * the older side of the accepted cut. It does not use ADWIN2 bucket
 * compression, so memory is bounded by {@link AdwinConfig#windowSize()}.</p>
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
        double[] currentValues = Arrays.copyOfRange(best.values(), best.split(), best.values().length);
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
                "ADWIN adaptive window cut exceeded the confidence bound",
                DriftEvents.standardDetails(
                        best.leftMean(),
                        best.rightMean(),
                        1.0,
                        config.criticalMultiplier(),
                        Map.of(
                                "epsilon", best.epsilon(),
                                "split", best.split(),
                                "windowSize", best.values().length,
                                "variance", best.variance(),
                                "harmonicMean", best.harmonicMean(),
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
        double variance = variance(values);
        double harmonicMean = 1.0 / (1.0 / left.length + 1.0 / right.length);
        double adjustedDelta = config.delta() / values.length;
        double logTerm = Math.log(2.0 / adjustedDelta);
        double epsilon = Math.sqrt(2.0 * variance * logTerm / harmonicMean) + (2.0 * logTerm / (3.0 * harmonicMean));
        if (diff <= epsilon) {
            return null;
        }
        return new Cut(values, split, leftMean, rightMean, variance, harmonicMean, epsilon, diff / epsilon);
    }

    private static double variance(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0.0);
        double sumSquares = 0.0;
        for (double value : values) {
            double deviation = value - mean;
            sumSquares += deviation * deviation;
        }
        return sumSquares / values.length;
    }

    private record Cut(
            double[] values,
            int split,
            double leftMean,
            double rightMean,
            double variance,
            double harmonicMean,
            double epsilon,
            double score
    ) {
        private Cut {
            if (split <= 0) {
                throw new IllegalArgumentException("split must be positive");
            }
            values = Arrays.copyOf(values, values.length);
        }
    }
}
