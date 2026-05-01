package ru.eljke.driftguard.algorithms.pagehinkley;

import ru.eljke.driftguard.algorithms.support.DriftEvents;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Map;

/**
 * Онлайн detector для роста скользящего среднего.
 *
 * <p>Реализация полезна для latency, processing time, queue size, CPU, memory
 * и других метрик, где подозрителен устойчивый рост.</p>
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
        double mean = state.mean() + config.alpha() * (point.value() - state.mean());
        double cumulative = state.cumulative() + point.value() - mean - config.delta();
        double minCumulative = Math.min(state.minCumulative(), cumulative);
        double score = cumulative - minCumulative;
        PageHinkleyState next = new PageHinkleyState(count, mean, cumulative, minCumulative);
        if (count < config.warmupSamples() || score < config.warningThreshold()) {
            return DetectionResult.noDrift(next);
        }
        return DetectionResult.drift(next, DriftEvents.create(
                point,
                context,
                name(),
                DriftDirection.UP,
                DriftEvents.severity(score, config.warningThreshold(), config.criticalThreshold()),
                score,
                point.value(),
                mean,
                "Page-Hinkley cumulative mean shift exceeded threshold",
                Map.of("count", count, "delta", config.delta())
        ));
    }
}
