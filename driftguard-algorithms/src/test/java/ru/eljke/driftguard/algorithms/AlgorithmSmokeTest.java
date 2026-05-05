package ru.eljke.driftguard.algorithms;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareConfig;
import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.psi.PsiConfig;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftEventPhase;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmSmokeTest {
    @Test
    void defaultRegistryContainsRequestedAlgorithms() {
        assertTrue(DefaultAlgorithms.registry().find("psi").isPresent());
        assertTrue(DefaultAlgorithms.registry().find("adwin").isPresent());
        assertTrue(DefaultAlgorithms.registry().find("page-hinkley").isPresent());
        assertTrue(DefaultAlgorithms.registry().find("ks").isPresent());
        assertTrue(DefaultAlgorithms.registry().find("chi-square").isPresent());
    }

    @Test
    void pageHinkleyDetectsUpwardMeanShiftByDefault() {
        assertDetects(new PageHinkleyConfig(10, 0.1, 10.0, 20.0, 0.05), stableThenShifted());
    }

    @Test
    void pageHinkleyDetectsDownwardMeanShiftWhenConfigured() {
        assertDetects(
                new PageHinkleyConfig(10, 0.1, 10.0, 20.0, 0.05, DriftDirection.DOWN),
                stableThenDropped(),
                DriftDirection.DOWN
        );
    }

    @Test
    void pageHinkleyKeepsPermanentShiftInSingleActiveEpisode() {
        DriftDetectorEngine engine = new DriftDetectorEngine(
                DefaultAlgorithms.registry(),
                new InMemoryDetectorStateStore(),
                List.of(new DetectorDefinition(
                        "page-hinkley-detector",
                        new PageHinkleyConfig(10, 0.1, 10.0, 20.0, 0.05),
                        key -> true,
                        new EmissionPolicyConfig(1, Duration.ofSeconds(10), 3)
                ))
        );
        MetricKey key = MetricKey.of("payments", "latency");
        List<DriftEvent> events = new ArrayList<>();
        double[] values = stableThenShifted();
        for (int i = 0; i < values.length; i++) {
            events.addAll(engine.detect(MetricPoint.gauge(
                    key,
                    Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i),
                    values[i]
            )));
        }

        assertEquals(1, events.stream().filter(event -> event.phase() == DriftEventPhase.STARTED).count());
        assertTrue(events.stream().anyMatch(event -> event.phase() == DriftEventPhase.ONGOING));
        assertFalse(events.stream().anyMatch(event -> event.phase() == DriftEventPhase.RECOVERED));
    }

    @Test
    void adwinDetectsMeanShift() {
        assertDetects(new AdwinConfig(40, 10, 0.2, 2.0), stableThenShifted());
    }

    @Test
    void adwinShrinksWindowAfterDetectedShift() {
        DriftDetectorEngine engine = new DriftDetectorEngine(
                DefaultAlgorithms.registry(),
                new InMemoryDetectorStateStore(),
                List.of(new DetectorDefinition("adwin-detector", new AdwinConfig(40, 10, 0.2, 2.0), key -> true))
        );
        MetricKey key = MetricKey.of("payments", "latency");
        List<DriftEvent> lastEvents = List.of();
        for (int i = 0; i < 70; i++) {
            double value = i < 35 ? 100.0 : 180.0;
            lastEvents = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i), value));
            if (!lastEvents.isEmpty()) {
                break;
            }
        }

        assertFalse(lastEvents.isEmpty());
        Object split = lastEvents.getFirst().details().get("split");
        Object windowSize = lastEvents.getFirst().details().get("windowSize");
        assertInstanceOf(Integer.class, split);
        assertInstanceOf(Integer.class, windowSize);
        assertTrue((Integer) split < (Integer) windowSize);
    }

    @Test
    void psiDetectsDistributionShift() {
        assertDetects(new PsiConfig(30, 30, 5, 0.1, 0.25, 1e-4), stableThenShifted());
    }

    @Test
    void ksDetectsDistributionShift() {
        assertDetects(new KsConfig(30, 30, 0.05, 0.01), stableThenShifted());
    }

    @Test
    void chiSquareDetectsDistributionShift() {
        assertDetects(new ChiSquareConfig(40, 40, 4, 0.05, 0.01, 1.0), stableThenShifted());
    }

    private static void assertDetects(DetectorConfig config, double[] values) {
        assertDetects(config, values, null);
    }

    private static void assertDetects(DetectorConfig config, double[] values, DriftDirection expectedDirection) {
        DriftDetectorEngine engine = new DriftDetectorEngine(
                DefaultAlgorithms.registry(),
                new InMemoryDetectorStateStore(),
                List.of(new DetectorDefinition(config.algorithm() + "-detector", config, key -> true))
        );
        MetricKey key = MetricKey.of("payments", "latency");
        DriftEvent detectedEvent = null;
        for (int i = 0; i < values.length; i++) {
            List<DriftEvent> events = engine.detect(MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i), values[i]));
            if (!events.isEmpty()) {
                detectedEvent = events.getFirst();
                break;
            }
        }
        assertNotNull(detectedEvent, "expected drift from " + config.algorithm());
        if (expectedDirection != null) {
            assertSame(detectedEvent.direction(), expectedDirection, "expected drift direction " + expectedDirection);
        }
    }

    private static double[] stableThenShifted() {
        double[] values = new double[100];
        for (int i = 0; i < 50; i++) {
            values[i] = 100.0 + (i % 5);
        }
        for (int i = 50; i < values.length; i++) {
            values[i] = 180.0 + (i % 5);
        }
        return values;
    }

    private static double[] stableThenDropped() {
        double[] values = new double[100];
        for (int i = 0; i < 50; i++) {
            values[i] = 180.0 + (i % 5);
        }
        for (int i = 50; i < values.length; i++) {
            values[i] = 100.0 + (i % 5);
        }
        return values;
    }
}
