package ru.eljke.driftguard.demo.detection;

import org.springframework.stereotype.Service;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.DriftGuard;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.config.MetricSelector;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * English demo documentation.
 * English demo documentation.
 */
@Service
public class DemoDetectionRuntime {
    private final AtomicLong versionSequence = new AtomicLong(1);
    private final AtomicReference<RuntimeState> state = new AtomicReference<>(create(DemoDetectorProfile.BALANCED, 1));

    public List<DriftEvent> detect(MetricPoint point) {
        return state.get().guard().detect(point);
    }

    public DemoDetectorProfile profile() {
        return state.get().profile();
    }

    public long version() {
        return state.get().version();
    }

    public List<DetectorDefinition> definitions() {
        return state.get().definitions();
    }

    public synchronized void reset() {
        state.set(create(profile(), versionSequence.incrementAndGet()));
    }

    public synchronized DemoDetectorProfile setProfile(DemoDetectorProfile profile) {
        state.set(create(profile, versionSequence.incrementAndGet()));
        return profile;
    }

    private static RuntimeState create(DemoDetectorProfile profile, long version) {
        List<DetectorDefinition> definitions = definitions(profile);
        return new RuntimeState(
                profile,
                version,
                definitions,
                DriftGuard.builder()
                        .registry(DefaultAlgorithms.registry())
                        .definitions(definitions)
                        .build()
        );
    }

    private static List<DetectorDefinition> definitions(DemoDetectorProfile profile) {
        ProfileSettings settings = ProfileSettings.of(profile);
        return List.of(
                pageHinkley(
                        "latency-page-hinkley",
                        "latency",
                        settings.latencyWarning(),
                        settings.latencyCritical(),
                        0.1,
                        DriftDirection.UP,
                        settings
                ),
                pageHinkley(
                        "error-rate-page-hinkley",
                        "error-rate",
                        settings.errorRateWarning(),
                        settings.errorRateCritical(),
                        0.001,
                        DriftDirection.UP,
                        settings
                ),
                pageHinkley(
                        "queue-size-page-hinkley",
                        "queue-size",
                        settings.queueWarning(),
                        settings.queueCritical(),
                        0.1,
                        DriftDirection.UP,
                        settings
                ),
                pageHinkley(
                        "throughput-page-hinkley",
                        "throughput",
                        settings.throughputWarning(),
                        settings.throughputCritical(),
                        1.0,
                        DriftDirection.DOWN,
                        settings
                )
        );
    }

    private static DetectorDefinition pageHinkley(
            String name,
            String metric,
            double warningThreshold,
            double criticalThreshold,
            double delta,
            DriftDirection direction,
            ProfileSettings settings
    ) {
        return DetectorDefinition.builder()
                .name(name)
                .config(PageHinkleyConfig.builder()
                        .warmupSamples(20)
                        .delta(delta)
                        .warningThreshold(warningThreshold)
                        .criticalThreshold(criticalThreshold)
                        .alpha(0.05)
                        .direction(direction)
                        .build())
                .appliesTo(MetricSelector.builder()
                        .metric(metric)
                        .build())
                .emissionPolicy(settings.emissionPolicy())
                .build();
    }

    private record RuntimeState(
            DemoDetectorProfile profile,
            long version,
            List<DetectorDefinition> definitions,
            DriftGuard guard
    ) {
    }

        private record ProfileSettings(
            double latencyWarning,
            double latencyCritical,
            double errorRateWarning,
            double errorRateCritical,
            double queueWarning,
            double queueCritical,
            double throughputWarning,
            double throughputCritical,
            EmissionPolicyConfig emissionPolicy
    ) {
        private static ProfileSettings of(DemoDetectorProfile profile) {
            return switch (profile) {
                case AGGRESSIVE -> new ProfileSettings(
                        25.0, 80.0,
                        0.025, 0.09,
                        25.0, 70.0,
                        90.0, 180.0,
                        emissionPolicy(2, Duration.ofSeconds(20))
                );
                case BALANCED -> new ProfileSettings(
                        35.0, 115.0,
                        0.045, 0.14,
                        35.0, 110.0,
                        120.0, 250.0,
                        emissionPolicy(2, Duration.ofSeconds(45))
                );
                case CONSERVATIVE -> new ProfileSettings(
                        70.0, 190.0,
                        0.07, 0.20,
                        75.0, 180.0,
                        180.0, 350.0,
                        emissionPolicy(4, Duration.ofSeconds(75))
                );
            };
        }

        private static EmissionPolicyConfig emissionPolicy(int minConsecutiveSignals, Duration cooldown) {
            return EmissionPolicyConfig.builder()
                    .minConsecutiveSignals(minConsecutiveSignals)
                    .cooldown(cooldown)
                    .recoveryConsecutiveNormal(1)
                    .build();
        }
    }
}


