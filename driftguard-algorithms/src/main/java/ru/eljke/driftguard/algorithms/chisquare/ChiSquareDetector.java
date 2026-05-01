package ru.eljke.driftguard.algorithms.chisquare;

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
        double statistic = 0.0;
        int usedBuckets = 0;
        for (int i = 0; i < buckets; i++) {
            double scaledExpected = expected[i] * (current.length / (double) baseline.length);
            if (scaledExpected < minExpectedCount) {
                continue;
            }
            double diff = observed[i] - scaledExpected;
            statistic += diff * diff / scaledExpected;
            usedBuckets++;
        }
        int degreesOfFreedom = Math.max(1, usedBuckets - 1);
        return new ChiSquareScore(statistic, chiSquareSurvival(statistic, degreesOfFreedom), degreesOfFreedom);
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

    private static double chiSquareSurvival(double x, int degreesOfFreedom) {
        return regularizedGammaQ(degreesOfFreedom / 2.0, x / 2.0);
    }

    private static double regularizedGammaQ(double a, double x) {
        if (x < 0 || a <= 0) {
            throw new IllegalArgumentException("invalid gamma arguments");
        }
        if (x == 0) {
            return 1.0;
        }
        if (x < a + 1.0) {
            return 1.0 - regularizedGammaPSeries(a, x);
        }
        double b = x + 1.0 - a;
        double c = 1.0 / 1e-30;
        double d = 1.0 / b;
        double h = d;
        for (int i = 1; i <= 100; i++) {
            double an = -i * (i - a);
            b += 2.0;
            d = an * d + b;
            if (Math.abs(d) < 1e-30) {
                d = 1e-30;
            }
            c = b + an / c;
            if (Math.abs(c) < 1e-30) {
                c = 1e-30;
            }
            d = 1.0 / d;
            double delta = d * c;
            h *= delta;
            if (Math.abs(delta - 1.0) < 1e-12) {
                break;
            }
        }
        return Math.exp(-x + a * Math.log(x) - logGamma(a)) * h;
    }

    private static double regularizedGammaPSeries(double a, double x) {
        double sum = 1.0 / a;
        double value = sum;
        for (int n = 1; n <= 100; n++) {
            value *= x / (a + n);
            sum += value;
            if (Math.abs(value) < Math.abs(sum) * 1e-12) {
                break;
            }
        }
        return sum * Math.exp(-x + a * Math.log(x) - logGamma(a));
    }

    private static double logGamma(double x) {
        double[] coefficients = {
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };
        if (x < 0.5) {
            return Math.log(Math.PI) - Math.log(Math.sin(Math.PI * x)) - logGamma(1.0 - x);
        }
        x -= 1.0;
        double a = 0.99999999999980993;
        for (int i = 0; i < coefficients.length; i++) {
            a += coefficients[i] / (x + i + 1.0);
        }
        double t = x + coefficients.length - 0.5;
        return 0.5 * Math.log(2.0 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    private record ChiSquareScore(double statistic, double pValue, int degreesOfFreedom) {
    }
}
