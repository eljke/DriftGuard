package ru.eljke.driftguard.core.config;

import java.util.List;

/**
 * Extension point for code-defined detector definitions.
 *
 * <p>Use this contract when detector configuration cannot be expressed by the
 * built-in {@code application.yml} properties, for example when a custom
 * algorithm has its own typed {@link DetectorConfig}. Core stays independent
 * from Spring and Kafka: infrastructure modules only collect providers and pass
 * their definitions to {@code DriftDetectorEngine}.</p>
 */
@FunctionalInterface
public interface DetectorDefinitionProvider {
    /**
     * Returns detector definitions contributed by this provider.
     */
    List<DetectorDefinition> definitions();
}
