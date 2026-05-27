package ru.eljke.driftguard.spring.autoconfigure;

import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareConfig;
import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.psi.PsiConfig;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.EmissionPolicyConfig;
import ru.eljke.driftguard.core.config.MetricSelector;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.spring.error.SpringDriftGuardErrorReason;

import java.util.List;
import java.util.Locale;

final class DetectorDefinitionFactory {
    private DetectorDefinitionFactory() {
    }

    static List<DetectorDefinition> create(DriftGuardProperties properties) {
        if (!properties.isDetectorsEnabled()) {
            return List.of();
        }
        if (properties.getDetectors().isEmpty()) {
            return defaultDefinitions();
        }
        return properties.getDetectors().stream()
                .filter(DriftGuardProperties.DetectorProperties::isEnabled)
                .map(DetectorDefinitionFactory::create)
                .toList();
    }

    private static DetectorDefinition create(DriftGuardProperties.DetectorProperties properties) {
        DetectorConfig config = config(properties);
        String name = properties.getName() == null || properties.getName().isBlank()
                ? config.algorithm() + "-detector"
                : properties.getName().trim();
        return new DetectorDefinition(name, config, selector(properties), emissionPolicy(properties));
    }

    private static EmissionPolicyConfig emissionPolicy(DriftGuardProperties.DetectorProperties properties) {
        DriftGuardProperties.EmissionPolicyProperties policy = properties.getEmissionPolicy();
        return new EmissionPolicyConfig(
                policy.getMinConsecutiveSignals(),
                policy.getCooldown(),
                policy.getRecoveryConsecutiveNormal()
        );
    }

    private static DetectorConfig config(DriftGuardProperties.DetectorProperties properties) {
        String algorithm = required(properties.getAlgorithm(), "detector algorithm").toLowerCase(Locale.ROOT);
        return switch (algorithm) {
            case PsiConfig.ALGORITHM -> new PsiConfig(
                    properties.getBaselineWindowSize(),
                    properties.getCurrentWindowSize(),
                    properties.getBuckets(),
                    properties.getWarningThreshold(),
                    properties.getCriticalThreshold(),
                    properties.getEpsilon()
            );
            case AdwinConfig.ALGORITHM -> new AdwinConfig(
                    properties.getWindowSize(),
                    properties.getMinSubWindowSize(),
                    properties.getDelta(),
                    properties.getCriticalMultiplier()
            );
            case PageHinkleyConfig.ALGORITHM -> new PageHinkleyConfig(
                    properties.getWarmupSamples(),
                    properties.getDelta(),
                    properties.getWarningThreshold(),
                    properties.getCriticalThreshold(),
                    properties.getAlpha(),
                    properties.getDirection()
            );
            case KsConfig.ALGORITHM -> new KsConfig(
                    properties.getBaselineWindowSize(),
                    properties.getCurrentWindowSize(),
                    properties.getWarningPValue(),
                    properties.getCriticalPValue()
            );
            case ChiSquareConfig.ALGORITHM -> new ChiSquareConfig(
                    properties.getBaselineWindowSize(),
                    properties.getCurrentWindowSize(),
                    properties.getBuckets(),
                    properties.getWarningPValue(),
                    properties.getCriticalPValue(),
                    properties.getMinExpectedCount()
            );
            default -> throw new DriftGuardValidationException(SpringDriftGuardErrorReason.UNSUPPORTED_ALGORITHM, algorithm);
        };
    }

    private static MetricSelector selector(DriftGuardProperties.DetectorProperties properties) {
        return MetricSelector.of(
                properties.getServices(),
                properties.getMetrics(),
                properties.getOperations(),
                properties.getInstances()
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DriftGuardValidationException(SpringDriftGuardErrorReason.REQUIRED_PROPERTY, field);
        }
        return value.trim();
    }

    private static List<DetectorDefinition> defaultDefinitions() {
        return List.of(
                new DetectorDefinition("latency-page-hinkley", new PageHinkleyConfig(20, 0.1, 25.0, 50.0, 0.05), key -> key.metric().equals("latency")),
                new DetectorDefinition("latency-ks", new KsConfig(40, 40, 0.05, 0.01), key -> key.metric().equals("latency"))
        );
    }
}
