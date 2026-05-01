package ru.eljke.driftguard.spring;

import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareConfig;
import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.psi.PsiConfig;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

final class DetectorDefinitionFactory {
    private DetectorDefinitionFactory() {
    }

    static List<DetectorDefinition> create(DriftGuardProperties properties) {
        if (properties.getDetectors().isEmpty()) {
            return defaultDefinitions();
        }
        return properties.getDetectors().stream()
                .map(DetectorDefinitionFactory::create)
                .toList();
    }

    private static DetectorDefinition create(DriftGuardProperties.DetectorProperties properties) {
        DetectorConfig config = config(properties);
        String name = properties.getName() == null || properties.getName().isBlank()
                ? config.algorithm() + "-detector"
                : properties.getName().trim();
        return new DetectorDefinition(name, config, selector(properties));
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
                    properties.getAlpha()
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
            default -> throw new IllegalArgumentException("Unsupported DriftGuard algorithm: " + algorithm);
        };
    }

    private static Predicate<MetricKey> selector(DriftGuardProperties.DetectorProperties properties) {
        List<String> services = normalized(properties.getServices());
        List<String> metrics = normalized(properties.getMetrics());
        return key -> (services.isEmpty() || services.contains(key.service()))
                && (metrics.isEmpty() || metrics.contains(key.metric()));
    }

    private static List<String> normalized(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values == null ? List.<String>of() : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim());
            }
        }
        return List.copyOf(result);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
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
