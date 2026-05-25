package ru.eljke.driftguard.core.config;

import java.util.List;

/**
 * Extension point for detector definitions declared in code.
 *
 * <p>Use it when detector configuration is inconvenient or impossible
 * to express through standard {@code application.yml}, properties, for example when
 * a custom algorithm has its own typed {@link DetectorConfig}.
 * Core remains independent of Spring and Kafka: infrastructure
 * modules only collect providers and pass their definitions to the engine.</p>
 */
@FunctionalInterface
public interface DetectorDefinitionProvider {
    /**
     * Returns detector definitions provided by this provider.
     */
    List<DetectorDefinition> definitions();
}

