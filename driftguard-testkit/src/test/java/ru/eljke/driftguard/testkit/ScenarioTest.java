package ru.eljke.driftguard.testkit;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTest {
    @Test
    void stepScenarioGeneratesExpectedDriftInterval() {
        MetricScenario scenario = new StepDriftScenario(
                "latency-step",
                ScenarioConfig.latency("orders", "POST /orders", 100),
                50,
                100.0,
                220.0,
                2.0
        );

        assertEquals(100, scenario.generate().size());
        assertEquals(1, scenario.expectedDrifts().size());
        assertFalse(scenario.expectedDrifts().getFirst().start().isBefore(scenario.generate().get(50).timestamp()));
    }

    @Test
    void additionalScenariosGenerateExpectedStreams() {
        MetricScenario spike = new PulseSpikeScenario(
                "error-rate-spike",
                ScenarioConfig.errorRate("orders", "POST /orders", 120),
                40,
                20,
                0.01,
                0.20,
                0.001
        );
        MetricScenario seasonal = new SeasonalNoiseScenario(
                "latency-seasonal",
                ScenarioConfig.latency("orders", "POST /orders", 120),
                120.0,
                20.0,
                30,
                2.0
        );
        MetricScenario throughputDrop = new ThroughputDropScenario(
                "throughput-drop",
                ScenarioConfig.throughput("orders", "POST /orders", 120),
                60,
                1_000.0,
                450.0,
                15.0
        );

        assertEquals(120, spike.generate().size());
        assertEquals(1, spike.expectedDrifts().size());
        assertEquals(120, seasonal.generate().size());
        assertEquals(0, seasonal.expectedDrifts().size());
        assertEquals(120, throughputDrop.generate().size());
        assertEquals(1, throughputDrop.expectedDrifts().size());
    }

    @Test
    void evaluatorComputesPrecisionRecallAndMisses() {
        MetricScenario scenario = new StepDriftScenario(
                "latency-step",
                ScenarioConfig.latency("orders", "POST /orders", 100),
                50,
                100.0,
                220.0,
                2.0
        );

        DetectionMetrics metrics = DetectionEvaluator.evaluate(scenario, List.of(
                eventAt(Instant.parse("2026-05-01T10:00:20Z")),
                eventAt(Instant.parse("2026-05-01T10:00:55Z"))
        ));

        assertEquals(2, metrics.events());
        assertEquals(1, metrics.truePositiveEvents());
        assertEquals(1, metrics.falsePositiveEvents());
        assertEquals(1, metrics.expectedDriftIntervals());
        assertEquals(1, metrics.detectedDriftIntervals());
        assertEquals(0, metrics.missedDriftIntervals());
        assertEquals(0.5, metrics.precision());
        assertEquals(1.0, metrics.recall());
    }


    @Test
    void qualityGatePassesWhenSummarySatisfiesThresholds() {
        DetectionBenchmarkReport report = DetectionBenchmarkRunner.report("quality", List.of(
                new DetectionBenchmarkResult("good", new DetectionMetrics(
                        2,
                        2,
                        0,
                        1,
                        1,
                        0,
                        true,
                        Duration.ofSeconds(5),
                        1.0,
                        1.0
                ))
        ));

        DetectionQualityGate gate = DetectionQualityGate.builder()
                .minPrecision(0.95)
                .minRecall(1.0)
                .maxFalsePositiveEvents(0)
                .maxMissedDriftIntervals(0)
                .maxMeanFirstDetectionDelay(Duration.ofSeconds(10))
                .build();

        DetectionQualityReport qualityReport = gate.evaluate(report);

        assertTrue(qualityReport.passed());
    }

    @Test
    void qualityGateFailsWhenSummaryViolatesThresholds() {
        DetectionBenchmarkReport report = DetectionBenchmarkRunner.report("quality", List.of(
                new DetectionBenchmarkResult("bad", new DetectionMetrics(
                        2,
                        1,
                        1,
                        1,
                        0,
                        1,
                        false,
                        Duration.ofSeconds(30),
                        0.5,
                        0.0
                ))
        ));

        DetectionQualityGate gate = DetectionQualityGate.builder()
                .minPrecision(0.9)
                .minRecall(0.9)
                .maxFalsePositiveEvents(0)
                .maxMissedDriftIntervals(0)
                .maxMeanFirstDetectionDelay(Duration.ofSeconds(10))
                .build();

        DetectionQualityGateException exception = assertThrows(
                DetectionQualityGateException.class,
                () -> gate.assertPassed(report)
        );

        assertFalse(exception.report().passed());
        assertEquals(5, exception.report().violations().size());
    }

    private static DriftEvent eventAt(Instant detectedAt) {
        return new DriftEvent(
                null,
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
