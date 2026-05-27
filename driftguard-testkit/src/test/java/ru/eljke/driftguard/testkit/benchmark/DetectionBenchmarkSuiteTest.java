package ru.eljke.driftguard.testkit.benchmark;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.*;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkCase;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkReport;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkSuite;
import ru.eljke.driftguard.testkit.quality.DetectionQualityGates;
import ru.eljke.driftguard.testkit.quality.DetectionQualityReport;
import ru.eljke.driftguard.testkit.scenario.MetricScenario;
import ru.eljke.driftguard.testkit.scenario.ScenarioConfig;
import ru.eljke.driftguard.testkit.scenario.StepDriftScenario;

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
            if (sampleIndex(point) == 55) {
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
                    if (sampleIndex(point) == 55) {
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

    private static int sampleIndex(MetricPoint point) {
        Object sample = point.attributes().get("sample");
        if (sample instanceof Number number) {
            return number.intValue();
        }
        if (sample instanceof String text) {
            return Integer.parseInt(text);
        }
        throw new IllegalStateException("sample attribute is missing");
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
