package ru.eljke.driftguard.algorithms.psi;

import ru.eljke.driftguard.algorithms.support.DriftEvents;
import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Arrays;
import java.util.Map;

/**
 * Detector на основе Population Stability Index для drift-а binned-распределений.
 */
public final class PsiDetector implements DetectorAlgorithm<PsiConfig, PsiState> {
    @Override
    public String name() {
        return PsiConfig.ALGORITHM;
    }

    @Override
    public Class<PsiConfig> configType() {
        return PsiConfig.class;
    }

    @Override
    public PsiState initialState(PsiConfig config) {
        return new PsiState(new SlidingDoubleWindow(config.baselineWindowSize()), new SlidingDoubleWindow(config.currentWindowSize()));
    }

    @Override
    public DetectionResult<PsiState> detect(MetricPoint point, PsiState state, PsiConfig config, DetectionContext context) {
        PsiState next;
        if (!state.baseline().isFull()) {
            next = new PsiState(state.baseline().add(point.value()), state.current());
            return DetectionResult.noDrift(next);
        }
        next = new PsiState(state.baseline(), state.current().add(point.value()));
        if (!next.current().isFull()) {
            return DetectionResult.noDrift(next);
        }

        double psi = psi(next.baseline().toArray(), next.current().toArray(), config.buckets(), config.epsilon());
        if (psi < config.warningThreshold()) {
            return DetectionResult.noDrift(next);
        }
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                DriftDirection.DISTRIBUTION,
                DriftEvents.severity(psi, config.warningThreshold(), config.criticalThreshold()),
                psi,
                next.current().mean(),
                next.baseline().mean(),
                "Population Stability Index exceeded threshold",
                Map.of("buckets", config.buckets())
        ));
    }

    private static double psi(double[] baseline, double[] current, int buckets, double epsilon) {
        double min = Math.min(Arrays.stream(baseline).min().orElse(0.0), Arrays.stream(current).min().orElse(0.0));
        double max = Math.max(Arrays.stream(baseline).max().orElse(0.0), Arrays.stream(current).max().orElse(0.0));
        if (Double.compare(min, max) == 0) {
            return 0.0;
        }
        double width = (max - min) / buckets;
        double score = 0.0;
        for (int i = 0; i < buckets; i++) {
            double lower = min + i * width;
            double upper = i == buckets - 1 ? max : lower + width;
            double expected = ratioInBucket(baseline, lower, upper, i == buckets - 1, epsilon);
            double actual = ratioInBucket(current, lower, upper, i == buckets - 1, epsilon);
            score += (actual - expected) * Math.log(actual / expected);
        }
        return score;
    }

    private static double ratioInBucket(double[] values, double lower, double upper, boolean includeUpper, double epsilon) {
        long count = Arrays.stream(values)
                .filter(value -> value >= lower && (includeUpper ? value <= upper : value < upper))
                .count();
        return Math.max(epsilon, count / (double) values.length);
    }
}
