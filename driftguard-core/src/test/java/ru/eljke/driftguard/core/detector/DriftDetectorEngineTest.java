package ru.eljke.driftguard.core.detector;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;

import java.time.Instant;
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
}
