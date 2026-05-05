package ru.eljke.driftguard.algorithms.pagehinkley;

import ru.eljke.driftguard.algorithms.support.DriftEvents;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Map;

/**
 * Онлайн detector для однонаправленного сдвига скользящего среднего.
 *
 * <p>По умолчанию detector ищет рост среднего. Для метрик вроде throughput,
 * где опасно устойчивое падение, используйте {@link DriftDirection#DOWN}.</p>
 */
public final class PageHinkleyDetector implements DetectorAlgorithm<PageHinkleyConfig, PageHinkleyState> {
    @Override
    public String name() {
        return PageHinkleyConfig.ALGORITHM;
    }

    @Override
    public Class<PageHinkleyConfig> configType() {
        return PageHinkleyConfig.class;
    }

    @Override
    public PageHinkleyState initialState(PageHinkleyConfig config) {
        return new PageHinkleyState(0, 0.0, 0.0, 0.0);
    }

    @Override
    public DetectionResult<PageHinkleyState> detect(
            MetricPoint point,
            PageHinkleyState state,
            PageHinkleyConfig config,
            DetectionContext context
    ) {
        long count = state.count() + 1;
        double observed = observedValue(point.value(), config.direction());

        if (count == 1) {
            return DetectionResult.noDrift(new PageHinkleyState(1, observed, 0.0, 0.0));
        }

        if (count <= config.warmupSamples()) {
            double warmupMean = state.mean() + (observed - state.mean()) / count;
            return DetectionResult.noDrift(new PageHinkleyState(count, warmupMean, 0.0, 0.0));
        }

        double mean = state.mean() + config.alpha() * (observed - state.mean());
        if (observed < mean) {
            return DetectionResult.noDrift(new PageHinkleyState(count, mean, 0.0, 0.0));
        }

        double cumulative = state.cumulative() + observed - mean - config.delta();
        double minCumulative = Math.min(state.minCumulative(), cumulative);
        double score = cumulative - minCumulative;
        PageHinkleyState next = new PageHinkleyState(count, mean, cumulative, minCumulative);

        if (score < config.warningThreshold()) {
            return DetectionResult.noDrift(next);
        }

        double baseline = originalValue(mean, config.direction());
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                config.direction(),
                DriftEvents.severity(score, config.warningThreshold(), config.criticalThreshold()),
                score,
                point.value(),
                baseline,
                "Page-Hinkley cumulative mean shift exceeded threshold",
                DriftEvents.standardDetails(
                        baseline,
                        point.value(),
                        config.warningThreshold(),
                        config.criticalThreshold(),
                        Map.of(
                                "count", count,
                                "delta", config.delta(),
                                "direction", config.direction().name(),
                                "cumulativeScore", score
                        )
                )
        ));
    }

    private static double observedValue(double value, DriftDirection direction) {
        return direction == DriftDirection.DOWN ? -value : value;
    }

    private static double originalValue(double value, DriftDirection direction) {
        return direction == DriftDirection.DOWN ? -value : value;
    }
}
