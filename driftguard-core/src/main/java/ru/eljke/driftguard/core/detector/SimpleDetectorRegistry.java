package ru.eljke.driftguard.core.detector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Неизменяемый in-memory registry для тестов, demo и Spring auto-configuration.
 */
public final class SimpleDetectorRegistry implements DetectorRegistry {
    private final Map<String, DetectorAlgorithm<?, ?>> algorithms;

    public SimpleDetectorRegistry(Collection<? extends DetectorAlgorithm<?, ?>> algorithms) {
        Map<String, DetectorAlgorithm<?, ?>> byName = new LinkedHashMap<>();
        for (DetectorAlgorithm<?, ?> algorithm : algorithms == null ? List.<DetectorAlgorithm<?, ?>>of() : algorithms) {
            DetectorAlgorithm<?, ?> previous = byName.putIfAbsent(algorithm.name(), algorithm);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate detector algorithm: " + algorithm.name());
            }
        }
        this.algorithms = Map.copyOf(byName);
    }

    @Override
    public Optional<DetectorAlgorithm<?, ?>> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(algorithms.get(name));
    }

    @Override
    public Collection<String> algorithmNames() {
        return algorithms.keySet();
    }
}
