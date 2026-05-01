package ru.eljke.driftguard.algorithms.ks;

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
 * Двухвыборочный detector Колмогорова-Смирнова для drift-а непрерывных распределений.
 */
public final class KsDetector implements DetectorAlgorithm<KsConfig, KsState> {
    @Override
    public String name() {
        return KsConfig.ALGORITHM;
    }

    @Override
    public Class<KsConfig> configType() {
        return KsConfig.class;
    }

    @Override
    public KsState initialState(KsConfig config) {
        return new KsState(new SlidingDoubleWindow(config.baselineWindowSize()), new SlidingDoubleWindow(config.currentWindowSize()));
    }

    @Override
    public DetectionResult<KsState> detect(MetricPoint point, KsState state, KsConfig config, DetectionContext context) {
        KsState next;
        if (!state.baseline().isFull()) {
            next = new KsState(state.baseline().add(point.value()), state.current());
            return DetectionResult.noDrift(next);
        }
        next = new KsState(state.baseline(), state.current().add(point.value()));
        if (!next.current().isFull()) {
            return DetectionResult.noDrift(next);
        }
        double statistic = statistic(next.baseline().toArray(), next.current().toArray());
        double pValue = approximatePValue(statistic, next.baseline().size(), next.current().size());
        if (pValue > config.warningPValue()) {
            return DetectionResult.noDrift(next);
        }
        DriftSeverity severity = pValue <= config.criticalPValue() ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                DriftDirection.DISTRIBUTION,
                severity,
                1.0 - pValue,
                next.current().mean(),
                next.baseline().mean(),
                "Kolmogorov-Smirnov distribution distance exceeded p-value threshold",
                Map.of("statistic", statistic, "pValue", pValue)
        ));
    }

    private static double statistic(double[] a, double[] b) {
        Arrays.sort(a);
        Arrays.sort(b);
        int i = 0;
        int j = 0;
        double max = 0.0;
        while (i < a.length && j < b.length) {
            double value = Math.min(a[i], b[j]);
            while (i < a.length && a[i] <= value) {
                i++;
            }
            while (j < b.length && b[j] <= value) {
                j++;
            }
            max = Math.max(max, Math.abs(i / (double) a.length - j / (double) b.length));
        }
        return max;
    }

    private static double approximatePValue(double d, int n, int m) {
        double effectiveN = n * m / (double) (n + m);
        double lambda = (Math.sqrt(effectiveN) + 0.12 + 0.11 / Math.sqrt(effectiveN)) * d;
        double sum = 0.0;
        for (int k = 1; k <= 100; k++) {
            double term = Math.pow(-1, k - 1) * Math.exp(-2.0 * k * k * lambda * lambda);
            sum += term;
            if (Math.abs(term) < 1e-8) {
                break;
            }
        }
        return Math.clamp(2.0 * sum, 0.0, 1.0);
    }
}
