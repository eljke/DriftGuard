package ru.eljke.driftguard.core;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.config.MetricSelector;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.SimpleDetectorRegistry;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriftGuardBuilderTest {
    @Test
    void buildsEmbeddableRuntimeWithMetricSelectorAndSink() {
        List<DriftEvent> published = new ArrayList<>();
        List<String> alertTitles = new ArrayList<>();
        DriftGuard guard = DriftGuard.builder()
                .registry(registry())
                .definition(DetectorDefinition.builder()
                        .name("latency-threshold")
                        .config(new ThresholdConfig(100.0))
                        .appliesTo(MetricSelector.builder()
                                .service("checkout-service")
                                .metric("latency")
                                .build())
                        .emissionPolicy(EmissionPolicyConfig.builder()
                                .cooldown(java.time.Duration.ZERO)
                                .build())
                        .build())
                .sink(published::add)
                .alertSink(alert -> alertTitles.add(alert.title()))
                .build();

        MetricPoint point = MetricPoint.builder()
                .key(MetricKey.builder()
                        .service("checkout-service")
                        .metric("latency")
                        .operation("POST /checkout")
                        .build())
                .timestamp(Instant.parse("2026-05-01T10:00:00Z"))
                .value(135.0)
                .kind(MetricKind.DURATION)
                .build();

        List<DriftEvent> events = guard.detect(point);

        assertEquals(1, events.size());
        assertEquals(events, published);
        assertEquals(1, alertTitles.size());
        assertEquals("latency-threshold", events.getFirst().detector());
    }

    private static DetectorRegistry registry() {
        return new SimpleDetectorRegistry(List.of(new ThresholdAlgorithm()));
    }

    private record ThresholdConfig(double threshold) implements DetectorConfig {
        @Override
        public String algorithm() {
            return "threshold";
        }
    }

    private record ThresholdState() implements DetectorState {
        @Override
        public String algorithm() {
            return "threshold";
        }
    }

    private static final class ThresholdAlgorithm implements DetectorAlgorithm<ThresholdConfig, ThresholdState> {
        @Override
        public String name() {
            return "threshold";
        }

        @Override
        public Class<ThresholdConfig> configType() {
            return ThresholdConfig.class;
        }

        @Override
        public ThresholdState initialState(ThresholdConfig config) {
            return new ThresholdState();
        }

        @Override
        public DetectionResult<ThresholdState> detect(
                MetricPoint point,
                ThresholdState state,
                ThresholdConfig config,
                DetectionContext context
        ) {
            if (point.value() <= config.threshold()) {
                return DetectionResult.noDrift(state);
            }
            return DetectionResult.drift(state, new DriftEvent(
                    null,
                    point.key(),
                    point.timestamp(),
                    point.timestamp(),
                    point.timestamp(),
                    DriftDirection.UP,
                    DriftSeverity.WARNING,
                    point.value() - config.threshold(),
                    point.value(),
                    config.threshold(),
                    context.detectorName(),
                    name(),
                    "threshold exceeded",
                    Map.of(),
                    Map.of()
            ));
        }
    }
}
