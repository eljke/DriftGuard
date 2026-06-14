package ru.eljke.driftguard.algorithms.adaptive;

import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyDetector;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyState;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdaptivePageHinkleyDetector
        implements DetectorAlgorithm<AdaptivePageHinkleyConfig, AdaptivePageHinkleyState> {
    private final PageHinkleyDetector delegate = new PageHinkleyDetector();

    @Override
    public String name() {
        return AdaptivePageHinkleyConfig.ALGORITHM;
    }

    @Override
    public Class<AdaptivePageHinkleyConfig> configType() {
        return AdaptivePageHinkleyConfig.class;
    }

    @Override
    public AdaptivePageHinkleyState initialState(AdaptivePageHinkleyConfig config) {
        return AdaptivePageHinkleyState.calibrating();
    }

    @Override
    public DetectionResult<AdaptivePageHinkleyState> detect(
            MetricPoint point,
            AdaptivePageHinkleyState state,
            AdaptivePageHinkleyConfig config,
            DetectionContext context
    ) {
        if (!state.calibrated()) {
            List<Double> baseline = new ArrayList<>(state.baselineValues());
            baseline.add(point.value());
            if (baseline.size() < config.calibrationSamples()) {
                return DetectionResult.noDrift(new AdaptivePageHinkleyState(baseline, null, null));
            }
            BaselineCharacteristics characteristics = BaselineCharacteristics.from(baseline);
            DetectorSensitivityProfile profile = config.selector().select(characteristics);
            PageHinkleyConfig selected = config.profile(profile);
            double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            double observedMean = selected.direction() == DriftDirection.DOWN ? -mean : mean;
            return DetectionResult.noDrift(new AdaptivePageHinkleyState(
                    List.of(),
                    profile,
                    new PageHinkleyState(baseline.size(), observedMean, 0.0, 0.0)
            ));
        }

        PageHinkleyConfig selected = config.profile(state.selectedProfile());
        DetectionResult<PageHinkleyState> result = delegate.detect(
                point,
                state.detectorState(),
                selected,
                context
        );
        AdaptivePageHinkleyState next = new AdaptivePageHinkleyState(
                List.of(),
                state.selectedProfile(),
                result.state()
        );
        return result.eventValue()
                .map(event -> DetectionResult.drift(next, withProfile(event, state.selectedProfile())))
                .orElseGet(() -> DetectionResult.noDrift(next));
    }

    private static DriftEvent withProfile(DriftEvent event, DetectorSensitivityProfile profile) {
        Map<String, Object> details = new LinkedHashMap<>(event.details());
        details.put("profileSelection", "ADAPTIVE");
        details.put("selectedProfile", profile.name());
        return new DriftEvent(
                event.id(),
                event.key(),
                event.detectedAt(),
                event.windowStart(),
                event.windowEnd(),
                event.phase(),
                event.direction(),
                event.severity(),
                event.score(),
                event.currentValue(),
                event.baselineValue(),
                event.detector(),
                AdaptivePageHinkleyConfig.ALGORITHM,
                event.reason(),
                event.tags(),
                details
        );
    }
}
