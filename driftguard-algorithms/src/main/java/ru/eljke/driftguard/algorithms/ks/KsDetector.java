package ru.eljke.driftguard.algorithms.ks;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import ru.eljke.driftguard.algorithms.support.DriftEvents;
import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Map;

/**
 * Двухвыборочный detector Колмогорова-Смирнова для drift-а непрерывных распределений.
 */
public final class KsDetector implements DetectorAlgorithm<KsConfig, KsState> {
    private static final KolmogorovSmirnovTest TEST = new KolmogorovSmirnovTest();

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
        double[] baseline = next.baseline().toArray();
        double[] current = next.current().toArray();
        double statistic = TEST.kolmogorovSmirnovStatistic(baseline, current);
        double pValue = TEST.kolmogorovSmirnovTest(baseline, current);
        if (pValue > config.warningPValue()) {
            return DetectionResult.noDrift(next);
        }
        DriftSeverity severity = pValue <= config.criticalPValue() ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
        double baselineMean = next.baseline().mean();
        double currentMean = next.current().mean();
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                DriftDirection.DISTRIBUTION,
                severity,
                1.0 - pValue,
                currentMean,
                baselineMean,
                "Kolmogorov-Smirnov distribution distance exceeded p-value threshold",
                DriftEvents.standardDetails(
                        baselineMean,
                        currentMean,
                        config.warningPValue(),
                        config.criticalPValue(),
                        Map.of("statistic", statistic, "pValue", pValue)
                )
        ));
    }
}
