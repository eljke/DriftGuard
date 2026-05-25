package ru.eljke.driftguard.demo.config;

import org.springframework.stereotype.Service;
import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.demo.detection.DemoDetectionRuntime;
import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;
import ru.eljke.driftguard.spring.DriftGuardProperties;

import java.util.List;

/**
 * English demo documentation.
 */
@Service
public class DemoConfigurationService {
    private final DemoKafkaProperties demoKafkaProperties;
    private final DriftGuardProperties driftGuardProperties;
    private final DemoDetectionRuntime runtime;
    private final DetectorRegistry detectorRegistry;

    public DemoConfigurationService(
            DemoKafkaProperties demoKafkaProperties,
            DriftGuardProperties driftGuardProperties,
            DemoDetectionRuntime runtime,
            DetectorRegistry detectorRegistry
    ) {
        this.demoKafkaProperties = demoKafkaProperties;
        this.driftGuardProperties = driftGuardProperties;
        this.runtime = runtime;
        this.detectorRegistry = detectorRegistry;
    }

    public DemoConfigurationView current() {
        List<DemoConfigurationView.DetectorConfigurationView> detectors = runtime.definitions().stream()
                .map(this::detectorView)
                .toList();
        return new DemoConfigurationView(
                aggressiveness(),
                List.of("AGGRESSIVE", "BALANCED", "CONSERVATIVE"),
                detectorRegistry.algorithmNames().stream().sorted().toList(),
                kafkaView(),
                detectors
        );
    }

    public DemoConfigurationView updateProfile(DemoDetectorProfile profile) {
        runtime.setProfile(profile);
        return current();
    }

    private DemoConfigurationView.KafkaConfigurationView kafkaView() {
        DriftGuardProperties.KafkaProperties kafka = driftGuardProperties.getKafka();
        return new DemoConfigurationView.KafkaConfigurationView(
                demoKafkaProperties.isEnabled(),
                demoKafkaProperties.getBootstrapServers(),
                demoKafkaProperties.getInputTopic(),
                demoKafkaProperties.getOutputTopic(),
                kafka.getApplicationId(),
                demoKafkaProperties.getPlaybackInterval()
        );
    }

    private DemoConfigurationView.DetectorConfigurationView detectorView(DetectorDefinition definition) {
        double warningThreshold = 0.0;
        double criticalThreshold = 0.0;
        double warningPValue = 0.0;
        double criticalPValue = 0.0;
        int warmupSamples = 0;
        if (definition.config() instanceof PageHinkleyConfig config) {
            warningThreshold = config.warningThreshold();
            criticalThreshold = config.criticalThreshold();
            warmupSamples = config.warmupSamples();
        }
        if (definition.config() instanceof KsConfig config) {
            warningPValue = config.warningPValue();
            criticalPValue = config.criticalPValue();
        }
        return new DemoConfigurationView.DetectorConfigurationView(
                definition.name(),
                definition.config().algorithm(),
                List.of(),
                metricFromName(definition.name()),
                warningThreshold,
                criticalThreshold,
                warningPValue,
                criticalPValue,
                warmupSamples,
                new DemoConfigurationView.EmissionPolicyView(
                        definition.emissionPolicy().minConsecutiveSignals(),
                        definition.emissionPolicy().cooldown(),
                        definition.emissionPolicy().recoveryConsecutiveNormal()
                ),
                runtime.profile().name() + " / v" + runtime.version()
        );
    }

    private DemoConfigurationView.AggressivenessView aggressiveness() {
        if (runtime.profile() == DemoDetectorProfile.AGGRESSIVE) {
            return new DemoConfigurationView.AggressivenessView(
                    "Aggressive",
                    "English demo text."
            );
        }
        if (runtime.profile() == DemoDetectorProfile.CONSERVATIVE) {
            return new DemoConfigurationView.AggressivenessView(
                    "Conservative",
                    "English demo text."
            );
        }
        return new DemoConfigurationView.AggressivenessView(
                "Balanced",
                "English demo text."
        );
    }

    private static List<String> metricFromName(String name) {
        if (name == null) {
            return List.of();
        }
        String[] parts = name.split("-");
        return parts.length == 0 ? List.of() : List.of(parts[0]);
    }
}


