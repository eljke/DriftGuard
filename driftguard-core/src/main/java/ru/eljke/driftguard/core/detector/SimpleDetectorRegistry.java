package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.error.CoreErrorReason;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable in-memory registry for tests, demos and Spring auto-configuration.
 */
public final class SimpleDetectorRegistry implements DetectorRegistry {
    private final Map<String, DetectorAlgorithm<?, ?>> algorithms;

    public SimpleDetectorRegistry(Collection<? extends DetectorAlgorithm<?, ?>> algorithms) {
        Map<String, DetectorAlgorithm<?, ?>> byName = new LinkedHashMap<>();
        for (DetectorAlgorithm<?, ?> algorithm : algorithms == null ? List.<DetectorAlgorithm<?, ?>>of() : algorithms) {
            DetectorAlgorithm<?, ?> previous = byName.putIfAbsent(algorithm.name(), algorithm);
            if (previous != null) {
                throw DriftGuardErrors.validation(CoreErrorReason.DUPLICATE_ALGORITHM, algorithm.name());
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

