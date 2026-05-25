package ru.eljke.driftguard.core.detector;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry that maps configured algorithm names to implementations.
 */
public interface DetectorRegistry {
    /**
     * Finds a registered algorithm by its stable name.
     */
    Optional<DetectorAlgorithm<?, ?>> find(String name);

    /**
     * Returns names of all registered algorithms.
     */
    Collection<String> algorithmNames();
}

