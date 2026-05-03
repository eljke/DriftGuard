package ru.eljke.driftguard.testkit;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionBenchmarkSuiteTest {
    @Test
    void runsCasesAndBuildsAggregatedReport() {
        DetectionBenchmarkSuite suite = DetectionBenchmarkSuite.of("suite", List.of(
                DetectionBenchmarkCase.of("step", stepScenario())
        ));

        DetectionBenchmarkReport report = suite.run(point -> {
            String sample = point.tags().get("sample");
            if ("55".equals(sample)) {
                return List.of(eventAt(point.timestamp()));
            }
            return List.of();
        });

        assertEquals("suite", report.label());
        assertEquals(1, report.results().size());
        assertEquals(1.0, report.summary().precision());
        assertEquals(1.0, report.summary().recall());
    }

    @Test
    void assertsQualityWithConfiguredGate() {
        DetectionBenchmarkSuite suite = DetectionBenchmarkSuite.of("suite", List.of(
                DetectionBenchmarkCase.of("step", stepScenario())
        ));

        DetectionQualityReport report = suite.assertQuality(
                point -> {
                    String sample = point.tags().get("sample");
                    if ("55".equals(sample)) {
                        return List.of(eventAt(point.timestamp()));
                    }
                    return List.of();
                },
                DetectionQualityGates.strict()
        );

        assertTrue(report.passed());
    }

    @Test
    void rejectsEmptySuite() {
        assertThrows(IllegalArgumentException.class, () -> DetectionBenchmarkSuite.of("empty", List.of()));
    }

    @Test
    void exposesReusableQualityGateProfiles() {
        assertTrue(DetectionQualityGates.smoke().minRecall() < DetectionQualityGates.strict().minRecall());
        assertTrue(DetectionQualityGates.balanced().maxFalsePositiveEvents()
                > DetectionQualityGates.strict().maxFalsePositiveEvents());
    }

    private static MetricScenario stepScenario() {
        return new StepDriftScenario(
                "latency-step",
                ScenarioConfig.latency("orders", "POST /orders", 80),
                50,
                100.0,
                220.0,
                0.0
        );
    }

    private static DriftEvent eventAt(Instant detectedAt) {
        return new DriftEvent(
                "event-1",
                MetricKey.of("orders", "latency"),
                detectedAt,
                detectedAt,
                detectedAt,
                DriftDirection.UP,
                DriftSeverity.WARNING,
                1.0,
                2.0,
                1.0,
                "test-detector",
                "test",
                "test event",
                Map.of(),
                Map.of()
        );
    }
}
