package ru.eljke.driftguard.demo.config;

import org.springframework.stereotype.Service;
import ru.eljke.driftguard.spring.DriftGuardProperties;

import java.util.List;

/**
 * Собирает безопасное представление конфигурации для demo UI.
 */
@Service
public class DemoConfigurationService {
    private final DemoKafkaProperties demoKafkaProperties;
    private final DriftGuardProperties driftGuardProperties;

    public DemoConfigurationService(DemoKafkaProperties demoKafkaProperties, DriftGuardProperties driftGuardProperties) {
        this.demoKafkaProperties = demoKafkaProperties;
        this.driftGuardProperties = driftGuardProperties;
    }

    public DemoConfigurationView current() {
        List<DemoConfigurationView.DetectorConfigurationView> detectors = driftGuardProperties.getDetectors().stream()
                .map(this::detectorView)
                .toList();
        return new DemoConfigurationView(
                aggressiveness(detectors),
                kafkaView(),
                detectors
        );
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

    private DemoConfigurationView.DetectorConfigurationView detectorView(DriftGuardProperties.DetectorProperties detector) {
        return new DemoConfigurationView.DetectorConfigurationView(
                detector.getName(),
                detector.getAlgorithm(),
                detector.getServices(),
                detector.getMetrics(),
                detector.getWarningThreshold(),
                detector.getCriticalThreshold(),
                detector.getWarningPValue(),
                detector.getCriticalPValue(),
                detector.getWarmupSamples(),
                new DemoConfigurationView.EmissionPolicyView(
                        detector.getEmissionPolicy().getMinConsecutiveSignals(),
                        detector.getEmissionPolicy().getCooldown()
                ),
                sensitivity(detector)
        );
    }

    private static DemoConfigurationView.AggressivenessView aggressiveness(
            List<DemoConfigurationView.DetectorConfigurationView> detectors
    ) {
        long aggressive = detectors.stream().filter(detector -> detector.sensitivity().equals("Aggressive")).count();
        long conservative = detectors.stream().filter(detector -> detector.sensitivity().equals("Conservative")).count();
        if (aggressive > conservative) {
            return new DemoConfigurationView.AggressivenessView(
                    "Aggressive",
                    "Detector-ы настроены на раннее обнаружение drift-а ценой большего риска ложных тревог."
            );
        }
        if (conservative > aggressive) {
            return new DemoConfigurationView.AggressivenessView(
                    "Conservative",
                    "Detector-ы требуют более сильного подтверждения drift-а и реже создают события."
            );
        }
        return new DemoConfigurationView.AggressivenessView(
                "Balanced",
                "Detector-ы используют умеренные пороги: быстрый сигнал сохраняется, но события сглаживаются emission policy."
        );
    }

    private static String sensitivity(DriftGuardProperties.DetectorProperties detector) {
        if (detector.getWarningPValue() >= 0.05 || detector.getWarningThreshold() <= 25.0) {
            return "Aggressive";
        }
        if (detector.getWarningPValue() <= 0.01 || detector.getWarningThreshold() >= 120.0) {
            return "Conservative";
        }
        return "Balanced";
    }
}
