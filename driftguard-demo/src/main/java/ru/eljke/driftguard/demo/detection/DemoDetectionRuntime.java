package ru.eljke.driftguard.demo.detection;

import org.springframework.stereotype.Service;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.InMemoryDetectorRuntimeStateStore;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime-обёртка над {@link DriftDetectorEngine}, позволяющая demo UI менять
 * профиль чувствительности без перезапуска Spring Boot приложения.
 */
@Service
public class DemoDetectionRuntime {
    private final AtomicLong versionSequence = new AtomicLong(1);
    private final AtomicReference<RuntimeState> state = new AtomicReference<>(create(DemoDetectorProfile.BALANCED, 1));

    public List<DriftEvent> detect(MetricPoint point) {
        return state.get().engine().detect(point);
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
                new DriftDetectorEngine(DefaultAlgorithms.registry(), new InMemoryDetectorRuntimeStateStore(), definitions)
        );
    }

    private static List<DetectorDefinition> definitions(DemoDetectorProfile profile) {
        ProfileSettings settings = ProfileSettings.of(profile);
        return List.of(
                new DetectorDefinition(
                        "latency-page-hinkley",
                        new PageHinkleyConfig(20, 0.1, settings.latencyWarning(), settings.latencyCritical(), 0.05),
                        key -> key.metric().equals("latency"),
                        settings.emissionPolicy()
                ),
                new DetectorDefinition(
                        "error-rate-adwin",
                        new AdwinConfig(48, 12, settings.adwinDelta(), settings.adwinCriticalMultiplier()),
                        key -> key.metric().equals("error-rate"),
                        settings.emissionPolicy()
                ),
                new DetectorDefinition(
                        "queue-size-page-hinkley",
                        new PageHinkleyConfig(20, 0.1, settings.queueWarning(), settings.queueCritical(), 0.05),
                        key -> key.metric().equals("queue-size"),
                        settings.emissionPolicy()
                ),
                new DetectorDefinition(
                        "throughput-page-hinkley",
                        new PageHinkleyConfig(
                                20,
                                1.0,
                                settings.throughputWarning(),
                                settings.throughputCritical(),
                                0.05,
                                DriftDirection.DOWN
                        ),
                        key -> key.metric().equals("throughput"),
                        settings.emissionPolicy()
                )
        );
    }

    private record RuntimeState(
            DemoDetectorProfile profile,
            long version,
            List<DetectorDefinition> definitions,
            DriftDetectorEngine engine
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
            double adwinDelta,
            double adwinCriticalMultiplier,
            EmissionPolicyConfig emissionPolicy
    ) {
        private static ProfileSettings of(DemoDetectorProfile profile) {
            return switch (profile) {
                case AGGRESSIVE -> new ProfileSettings(
                        25.0, 80.0,
                        0.025, 0.09,
                        25.0, 70.0,
                        90.0, 180.0,
                        0.25, 1.8,
                        new EmissionPolicyConfig(2, Duration.ofSeconds(20))
                );
                case BALANCED -> new ProfileSettings(
                        35.0, 115.0,
                        0.045, 0.14,
                        35.0, 110.0,
                        120.0, 250.0,
                        0.20, 2.0,
                        new EmissionPolicyConfig(2, Duration.ofSeconds(45))
                );
                case CONSERVATIVE -> new ProfileSettings(
                        70.0, 190.0,
                        0.07, 0.20,
                        75.0, 180.0,
                        180.0, 350.0,
                        0.12, 2.4,
                        new EmissionPolicyConfig(4, Duration.ofSeconds(75))
                );
            };
        }
    }
}
