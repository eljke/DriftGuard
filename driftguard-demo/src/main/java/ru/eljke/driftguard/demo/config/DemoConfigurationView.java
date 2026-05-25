package ru.eljke.driftguard.demo.config;

import java.time.Duration;
import java.util.List;

/**
 * English demo documentation.
 * English demo documentation.
 *
 * @param aggressiveness documented value
 * @param availableProfiles documented value
 * @param registeredAlgorithms documented value
 * @param kafka documented value
 * @param detectors documented value
 */
public record DemoConfigurationView(
        AggressivenessView aggressiveness,
        List<String> availableProfiles,
        List<String> registeredAlgorithms,
        KafkaConfigurationView kafka,
        List<DetectorConfigurationView> detectors
) {
    /**
     * English demo documentation.
     *
     * @param level documented value
     * @param description documented value
     */
    public record AggressivenessView(String level, String description) {
    }

    /**
     * English demo documentation.
     *
     * @param demoEnabled documented value
     * @param bootstrapServers Kafka bootstrap servers
     * @param inputTopic documented value
     * @param outputTopic documented value
     * @param applicationId Kafka Streams application id
     * @param playbackInterval documented value
     */
    public record KafkaConfigurationView(
            boolean demoEnabled,
            String bootstrapServers,
            String inputTopic,
            String outputTopic,
            String applicationId,
            Duration playbackInterval
    ) {
    }

    /**
     * English demo documentation.
     *
     * @param name documented value
     * @param algorithm documented value
     * @param services documented value
     * @param metrics documented value
     * @param warningThreshold documented value
     * @param criticalThreshold documented value
     * @param warningPValue documented value
     * @param criticalPValue documented value
     * @param warmupSamples documented value
     * @param emissionPolicy documented value
     * @param sensitivity documented value
     */
    public record DetectorConfigurationView(
            String name,
            String algorithm,
            List<String> services,
            List<String> metrics,
            double warningThreshold,
            double criticalThreshold,
            double warningPValue,
            double criticalPValue,
            int warmupSamples,
            EmissionPolicyView emissionPolicy,
            String sensitivity
    ) {
    }

    /**
     * English demo documentation.
     *
     * @param minConsecutiveSignals documented value
     * @param cooldown documented value
     * @param recoveryConsecutiveNormal documented value
     */
    public record EmissionPolicyView(int minConsecutiveSignals, Duration cooldown, int recoveryConsecutiveNormal) {
    }
}


