package ru.eljke.driftguard.core.detector;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftEventPhase;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriftDetectorEngineTest {
    @Test
    void delegatesToMatchingDetectorAndPersistsState() {
        CountingAlgorithm algorithm = new CountingAlgorithm();
        InMemoryDetectorStateStore stateStore = new InMemoryDetectorStateStore();
        DetectorDefinition definition = new DetectorDefinition("counting", new CountingConfig(3), key -> key.metric().equals("latency"));
        DriftDetectorEngine engine = new DriftDetectorEngine(
                new SimpleDetectorRegistry(List.of(algorithm)),
                stateStore,
                List.of(definition)
        );

        MetricKey key = MetricKey.of("orders", "latency");
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z"), 100)).isEmpty());
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:01Z"), 101)).isEmpty());

        List<DriftEvent> events = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:02Z"), 102));

        assertEquals(1, events.size());
        assertEquals("counting", events.getFirst().detector());
        assertEquals(3, ((CountingState) stateStore.get(new DetectorInstanceKey(key, "counting")).orElseThrow()).count());
    }

    @Test
    void appliesConsecutiveSignalAndCooldownPolicy() {
        AlwaysDriftingAlgorithm algorithm = new AlwaysDriftingAlgorithm();
        DetectorDefinition definition = new DetectorDefinition(
                "always",
                new AlwaysDriftingConfig(),
                key -> true,
                new EmissionPolicyConfig(2, Duration.ofSeconds(10))
        );
        DriftDetectorEngine engine = new DriftDetectorEngine(
                new SimpleDetectorRegistry(List.of(algorithm)),
                new InMemoryDetectorStateStore(),
                List.of(definition)
        );
        MetricKey key = MetricKey.of("orders", "latency");

        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z"), 100)).isEmpty());
        assertEquals(1, engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:01Z"), 101)).size());
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:02Z"), 102)).isEmpty());
        List<DriftEvent> ongoingEvents = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:12Z"), 103));

        assertEquals(1, ongoingEvents.size());
        assertEquals(DriftEventPhase.ONGOING, ongoingEvents.getFirst().phase());
    }

    @Test
    void recoversDownwardEpisodeOnlyAfterMetricReturnsNearBaseline() {
        OneShotDriftAlgorithm algorithm = new OneShotDriftAlgorithm();
        DetectorDefinition definition = new DetectorDefinition(
                "one-shot",
                new OneShotDriftConfig(DriftDirection.DOWN, 1000.0),
                key -> true,
                new EmissionPolicyConfig(1, Duration.ZERO, 2)
        );
        DriftDetectorEngine engine = new DriftDetectorEngine(
                new SimpleDetectorRegistry(List.of(algorithm)),
                new InMemoryDetectorStateStore(),
                List.of(definition)
        );
        MetricKey key = MetricKey.of("checkout-service", "throughput");

        assertEquals(DriftEventPhase.STARTED, engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z"), 430)).getFirst().phase());
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:01Z"), 440)).isEmpty());
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:02Z"), 460)).isEmpty());
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:03Z"), 950)).isEmpty());

        List<DriftEvent> recovered = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:04Z"), 980));

        assertEquals(1, recovered.size());
        assertEquals(DriftEventPhase.RECOVERED, recovered.getFirst().phase());
    }

    @Test
    void doesNotRecoverDownwardEpisodeOnOppositeSignalFarFromBaseline() {
        OppositeSignalAlgorithm algorithm = new OppositeSignalAlgorithm();
        DetectorDefinition definition = new DetectorDefinition(
                "opposite",
                new OppositeSignalConfig(),
                key -> true,
                new EmissionPolicyConfig(1, Duration.ZERO, 1)
        );
        DriftDetectorEngine engine = new DriftDetectorEngine(
                new SimpleDetectorRegistry(List.of(algorithm)),
                new InMemoryDetectorStateStore(),
                List.of(definition)
        );
        MetricKey key = MetricKey.of("checkout-service", "throughput");

        assertEquals(DriftEventPhase.STARTED, engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z"), 430)).getFirst().phase());
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:01Z"), 460)).isEmpty());

        List<DriftEvent> recovered = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:02Z"), 980));

        assertEquals(1, recovered.size());
        assertEquals(DriftEventPhase.RECOVERED, recovered.getFirst().phase());
        assertEquals(980.0, recovered.getFirst().currentValue());
    }

    @Test
    void keepsEpisodeBaselineAcrossOngoingSignals() {
        BaselineChangingAlgorithm algorithm = new BaselineChangingAlgorithm();
        DetectorDefinition definition = new DetectorDefinition(
                "baseline-changing",
                new BaselineChangingConfig(),
                key -> true,
                new EmissionPolicyConfig(1, Duration.ZERO, 1)
        );
        DriftDetectorEngine engine = new DriftDetectorEngine(
                new SimpleDetectorRegistry(List.of(algorithm)),
                new InMemoryDetectorStateStore(),
                List.of(definition)
        );
        MetricKey key = MetricKey.of("checkout-service", "throughput");

        DriftEvent started = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z"), 430)).getFirst();
        DriftEvent ongoing = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:01Z"), 440)).getFirst();
        assertTrue(engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:02Z"), 460)).isEmpty());

        DriftEvent recovered = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:03Z"), 980)).getFirst();

        assertEquals(1000.0, started.baselineValue());
        assertEquals(1000.0, ongoing.baselineValue());
        assertEquals(DriftEventPhase.RECOVERED, recovered.phase());
    }

    private record CountingConfig(int emitAt) implements DetectorConfig {
        @Override
        public String algorithm() {
            return "counting";
        }
    }

    private record CountingState(int count) implements DetectorState {
        @Override
        public String algorithm() {
            return "counting";
        }
    }

    private static final class CountingAlgorithm implements DetectorAlgorithm<CountingConfig, CountingState> {
        @Override
        public String name() {
            return "counting";
        }

        @Override
        public Class<CountingConfig> configType() {
            return CountingConfig.class;
        }

        @Override
        public CountingState initialState(CountingConfig config) {
            return new CountingState(0);
        }

        @Override
        public DetectionResult<CountingState> detect(
                MetricPoint point,
                CountingState state,
                CountingConfig config,
                DetectionContext context
        ) {
            CountingState next = new CountingState(state.count() + 1);
            if (next.count() < config.emitAt()) {
                return DetectionResult.noDrift(next);
            }
            return DetectionResult.drift(next, new DriftEvent(
                    null,
                    point.key(),
                    point.timestamp(),
                    point.timestamp(),
                    point.timestamp(),
                    DriftDirection.UP,
                    DriftSeverity.WARNING,
                    next.count(),
                    point.value(),
                    0,
                    context.detectorName(),
                    name(),
                    "test",
                    Map.of(),
                    Map.of()
            ));
        }
    }

    private record AlwaysDriftingConfig() implements DetectorConfig {
        @Override
        public String algorithm() {
            return "always-drifting";
        }
    }

    private record AlwaysDriftingState() implements DetectorState {
        @Override
        public String algorithm() {
            return "always-drifting";
        }
    }

    private static final class AlwaysDriftingAlgorithm implements DetectorAlgorithm<AlwaysDriftingConfig, AlwaysDriftingState> {
        @Override
        public String name() {
            return "always-drifting";
        }

        @Override
        public Class<AlwaysDriftingConfig> configType() {
            return AlwaysDriftingConfig.class;
        }

        @Override
        public AlwaysDriftingState initialState(AlwaysDriftingConfig config) {
            return new AlwaysDriftingState();
        }

        @Override
        public DetectionResult<AlwaysDriftingState> detect(
                MetricPoint point,
                AlwaysDriftingState state,
                AlwaysDriftingConfig config,
                DetectionContext context
        ) {
            return DetectionResult.drift(state, new DriftEvent(
                    null,
                    point.key(),
                    point.timestamp(),
                    point.timestamp(),
                    point.timestamp(),
                    DriftDirection.UP,
                    DriftSeverity.WARNING,
                    1,
                    point.value(),
                    0,
                    context.detectorName(),
                    name(),
                    "test",
                    Map.of(),
                    Map.of()
            ));
        }
    }

    private record OneShotDriftConfig(DriftDirection direction, double baselineValue) implements DetectorConfig {
        @Override
        public String algorithm() {
            return "one-shot-drift";
        }
    }

    private record OneShotDriftState(boolean emitted) implements DetectorState {
        @Override
        public String algorithm() {
            return "one-shot-drift";
        }
    }

    private static final class OneShotDriftAlgorithm implements DetectorAlgorithm<OneShotDriftConfig, OneShotDriftState> {
        @Override
        public String name() {
            return "one-shot-drift";
        }

        @Override
        public Class<OneShotDriftConfig> configType() {
            return OneShotDriftConfig.class;
        }

        @Override
        public OneShotDriftState initialState(OneShotDriftConfig config) {
            return new OneShotDriftState(false);
        }

        @Override
        public DetectionResult<OneShotDriftState> detect(
                MetricPoint point,
                OneShotDriftState state,
                OneShotDriftConfig config,
                DetectionContext context
        ) {
            if (state.emitted()) {
                return DetectionResult.noDrift(state);
            }
            OneShotDriftState next = new OneShotDriftState(true);
            return DetectionResult.drift(next, new DriftEvent(
                    null,
                    point.key(),
                    point.timestamp(),
                    point.timestamp(),
                    point.timestamp(),
                    config.direction(),
                    DriftSeverity.CRITICAL,
                    1,
                    point.value(),
                    config.baselineValue(),
                    context.detectorName(),
                    name(),
                    "test",
                    Map.of(),
                    Map.of()
            ));
        }
    }

    private record OppositeSignalConfig() implements DetectorConfig {
        @Override
        public String algorithm() {
            return "opposite-signal";
        }
    }

    private record OppositeSignalState(int count) implements DetectorState {
        @Override
        public String algorithm() {
            return "opposite-signal";
        }
    }

    private static final class OppositeSignalAlgorithm implements DetectorAlgorithm<OppositeSignalConfig, OppositeSignalState> {
        @Override
        public String name() {
            return "opposite-signal";
        }

        @Override
        public Class<OppositeSignalConfig> configType() {
            return OppositeSignalConfig.class;
        }

        @Override
        public OppositeSignalState initialState(OppositeSignalConfig config) {
            return new OppositeSignalState(0);
        }

        @Override
        public DetectionResult<OppositeSignalState> detect(
                MetricPoint point,
                OppositeSignalState state,
                OppositeSignalConfig config,
                DetectionContext context
        ) {
            OppositeSignalState next = new OppositeSignalState(state.count() + 1);
            DriftDirection direction = next.count() == 1 ? DriftDirection.DOWN : DriftDirection.UP;
            return DetectionResult.drift(next, new DriftEvent(
                    null,
                    point.key(),
                    point.timestamp(),
                    point.timestamp(),
                    point.timestamp(),
                    direction,
                    DriftSeverity.CRITICAL,
                    1,
                    point.value(),
                    1000.0,
                    context.detectorName(),
                    name(),
                    "test",
                    Map.of(),
                    Map.of()
            ));
        }
    }

    private record BaselineChangingConfig() implements DetectorConfig {
        @Override
        public String algorithm() {
            return "baseline-changing";
        }
    }

    private record BaselineChangingState(int count) implements DetectorState {
        @Override
        public String algorithm() {
            return "baseline-changing";
        }
    }

    private static final class BaselineChangingAlgorithm implements DetectorAlgorithm<BaselineChangingConfig, BaselineChangingState> {
        @Override
        public String name() {
            return "baseline-changing";
        }

        @Override
        public Class<BaselineChangingConfig> configType() {
            return BaselineChangingConfig.class;
        }

        @Override
        public BaselineChangingState initialState(BaselineChangingConfig config) {
            return new BaselineChangingState(0);
        }

        @Override
        public DetectionResult<BaselineChangingState> detect(
                MetricPoint point,
                BaselineChangingState state,
                BaselineChangingConfig config,
                DetectionContext context
        ) {
            BaselineChangingState next = new BaselineChangingState(state.count() + 1);
            double baseline = next.count() == 1 ? 1000.0 : 430.0;
            DriftDirection direction = next.count() <= 2 ? DriftDirection.DOWN : DriftDirection.UP;
            return DetectionResult.drift(next, new DriftEvent(
                    null,
                    point.key(),
                    point.timestamp(),
                    point.timestamp(),
                    point.timestamp(),
                    direction,
                    DriftSeverity.CRITICAL,
                    1,
                    point.value(),
                    baseline,
                    context.detectorName(),
                    name(),
                    "test",
                    Map.of(),
                    Map.of()
            ));
        }
    }
}
