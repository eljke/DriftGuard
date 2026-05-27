package ru.eljke.driftguard.algorithms.quality;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.adwin.AdwinDetector;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyDetector;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkCase;
import ru.eljke.driftguard.testkit.benchmark.DetectionBenchmarkSuite;
import ru.eljke.driftguard.testkit.quality.DetectionQualityGate;
import ru.eljke.driftguard.testkit.scenario.ScenarioConfig;
import ru.eljke.driftguard.testkit.scenario.StepDriftScenario;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression-suite для базового качества встроенных detector-ов.
 *
 * <p>Smoke-тесты проверяют только факт события. Этот suite фиксирует минимальные
 * требования к precision, recall, false positives и задержке первого обнаружения
 * на воспроизводимых synthetic scenarios.</p>
 */
class BuiltInAlgorithmQualityGateTest {
    private static final DetectionQualityGate STEP_DRIFT_GATE = DetectionQualityGate.builder()
            .minPrecision(0.95)
            .minRecall(1.0)
            .maxFalsePositiveEvents(0)
            .maxMissedDriftIntervals(0)
            .maxMeanFirstDetectionDelay(Duration.ofSeconds(45))
            .build();

    @Test
    void pageHinkleyPassesStepDriftQualityGate() {
        DetectionBenchmarkSuite suite = DetectionBenchmarkSuite.of("page-hinkley-step-drift", List.of(
                DetectionBenchmarkCase.of("latency-step-up", latencyStepUpScenario())
        ));

        var report = suite.assertQuality(
                detector(new PageHinkleyDetector(), new PageHinkleyConfig(20, 0.1, 25.0, 50.0, 0.05), "latency-page-hinkley"),
                STEP_DRIFT_GATE
        );

        assertEquals(1.0, report.benchmarkReport().summary().precision());
        assertEquals(1.0, report.benchmarkReport().summary().recall());
    }

    @Test
    void adwinPassesStepDriftQualityGate() {
        DetectionBenchmarkSuite suite = DetectionBenchmarkSuite.of("adwin-step-drift", List.of(
                DetectionBenchmarkCase.of("latency-step-up", latencyStepUpScenario())
        ));

        var report = suite.assertQuality(
                detector(new AdwinDetector(), new AdwinConfig(48, 12, 0.2, 2.0), "latency-adwin"),
                STEP_DRIFT_GATE
        );

        assertEquals(1.0, report.benchmarkReport().summary().precision());
        assertEquals(1.0, report.benchmarkReport().summary().recall());
    }

    @Test
    void emittedEventsContainRequiredDiagnosticFields() {
        Function<MetricPoint, List<DriftEvent>> detector = detector(
                new PageHinkleyDetector(),
                new PageHinkleyConfig(20, 0.1, 25.0, 50.0, 0.05),
                "latency-page-hinkley"
        );

        DriftEvent event = latencyStepUpScenario().generate().stream()
                .flatMap(point -> detector.apply(point).stream())
                .findFirst()
                .orElseThrow();

        assertEquals("latency-page-hinkley", event.detector());
        assertEquals(PageHinkleyConfig.ALGORITHM, event.algorithm());
        assertNotNull(event.reason());
        assertNotNull(event.details().get("baselineMean"));
        assertNotNull(event.details().get("currentMean"));
        assertNotNull(event.details().get("cumulativeScore"));
    }

    private static StepDriftScenario latencyStepUpScenario() {
        return new StepDriftScenario(
                "latency-step-up",
                ScenarioConfig.latency("orders", "checkout", 140),
                70,
                100.0,
                200.0,
                0.0
        );
    }

    private static <C extends DetectorConfig, S extends DetectorState> Function<MetricPoint, List<DriftEvent>> detector(
            DetectorAlgorithm<C, S> algorithm,
            C config,
            String detectorName
    ) {
        AtomicReference<S> state = new AtomicReference<>(algorithm.initialState(config));
        DetectionContext context = new DetectionContext(detectorName);
        return point -> {
            DetectionResult<S> result = algorithm.detect(point, state.get(), config, context);
            state.set(result.state());
            return result.eventValue().stream().toList();
        };
    }
}
