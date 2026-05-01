package ru.eljke.driftguard.algorithms.chisquare;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
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
 * Хи-квадрат detector для изменений binned-распределения.
 *
 * <p>Этот detector наиболее полезен, когда метрику можно осмысленно разбить
 * на bucket-ы и в каждом bucket-е достаточно expected-наблюдений.</p>
 */
public final class ChiSquareDetector implements DetectorAlgorithm<ChiSquareConfig, ChiSquareState> {
    private static final ChiSquareTest TEST = new ChiSquareTest();

    @Override
    public String name() {
        return ChiSquareConfig.ALGORITHM;
    }

    @Override
    public Class<ChiSquareConfig> configType() {
        return ChiSquareConfig.class;
    }

    @Override
    public ChiSquareState initialState(ChiSquareConfig config) {
        return new ChiSquareState(new SlidingDoubleWindow(config.baselineWindowSize()), new SlidingDoubleWindow(config.currentWindowSize()));
    }

    @Override
    public DetectionResult<ChiSquareState> detect(
            MetricPoint point,
            ChiSquareState state,
            ChiSquareConfig config,
            DetectionContext context
    ) {
        ChiSquareState next;
        if (!state.baseline().isFull()) {
            next = new ChiSquareState(state.baseline().add(point.value()), state.current());
            return DetectionResult.noDrift(next);
        }
        next = new ChiSquareState(state.baseline(), state.current().add(point.value()));
        if (!next.current().isFull()) {
            return DetectionResult.noDrift(next);
        }
        ChiSquareScore score = score(next.baseline().toArray(), next.current().toArray(), config.buckets(), config.minExpectedCount());
        if (score.pValue() > config.warningPValue()) {
            return DetectionResult.noDrift(next);
        }
        DriftSeverity severity = score.pValue() <= config.criticalPValue() ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                DriftDirection.DISTRIBUTION,
                severity,
                1.0 - score.pValue(),
                next.current().mean(),
                next.baseline().mean(),
                "Chi-square binned distribution test exceeded p-value threshold",
                Map.of("chiSquare", score.statistic(), "pValue", score.pValue(), "degreesOfFreedom", score.degreesOfFreedom())
        ));
    }

    private static ChiSquareScore score(double[] baseline, double[] current, int buckets, double minExpectedCount) {
        double min = Math.min(Arrays.stream(baseline).min().orElse(0.0), Arrays.stream(current).min().orElse(0.0));
        double max = Math.max(Arrays.stream(baseline).max().orElse(0.0), Arrays.stream(current).max().orElse(0.0));
        if (Double.compare(min, max) == 0) {
            return new ChiSquareScore(0.0, 1.0, buckets - 1);
        }
        int[] expected = histogram(baseline, buckets, min, max);
        int[] observed = histogram(current, buckets, min, max);
        double[] smoothedExpected = new double[buckets];
        long[] observedCounts = new long[buckets];
        for (int i = 0; i < buckets; i++) {
            double scaledExpected = expected[i] * (current.length / (double) baseline.length);
            smoothedExpected[i] = Math.max(minExpectedCount, scaledExpected);
            observedCounts[i] = observed[i];
        }
        double statistic = TEST.chiSquare(smoothedExpected, observedCounts);
        double pValue = TEST.chiSquareTest(smoothedExpected, observedCounts);
        return new ChiSquareScore(statistic, pValue, Math.max(1, buckets - 1));
    }

    private static int[] histogram(double[] values, int buckets, double min, double max) {
        int[] result = new int[buckets];
        double width = (max - min) / buckets;
        for (double value : values) {
            int bucket = (int) ((value - min) / width);
            result[Math.clamp(bucket, 0, buckets - 1)]++;
        }
        return result;
    }

    private record ChiSquareScore(double statistic, double pValue, int degreesOfFreedom) {
    }
}
