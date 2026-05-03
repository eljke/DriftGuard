package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Неизменяемая in-memory реализация {@link DetectorStateCodecRegistry}.
 */
public final class SimpleDetectorStateCodecRegistry implements DetectorStateCodecRegistry {
    private final Map<String, DetectorStateCodec<?>> byAlgorithm;
    private final Map<Class<?>, DetectorStateCodec<?>> byStateType;

    public SimpleDetectorStateCodecRegistry(Collection<? extends DetectorStateCodec<?>> codecs) {
        Map<String, DetectorStateCodec<?>> algorithms = new LinkedHashMap<>();
        Map<Class<?>, DetectorStateCodec<?>> stateTypes = new LinkedHashMap<>();
        for (DetectorStateCodec<?> codec : codecs == null ? List.<DetectorStateCodec<?>>of() : codecs) {
            DetectorStateCodec<?> nonNullCodec = DriftGuardErrors.requireNonNull(codec, "codec");
            String algorithm = DriftGuardErrors.requireNonBlank(nonNullCodec.algorithm(), "codec.algorithm");
            Class<?> stateType = DriftGuardErrors.requireNonNull(nonNullCodec.stateType(), "codec.stateType");
            DetectorStateCodec<?> previousAlgorithm = algorithms.putIfAbsent(algorithm, nonNullCodec);
            if (previousAlgorithm != null) {
                throw new IllegalArgumentException("Duplicate detector state codec for algorithm: " + algorithm);
            }
            DetectorStateCodec<?> previousStateType = stateTypes.putIfAbsent(stateType, nonNullCodec);
            if (previousStateType != null) {
                throw new IllegalArgumentException("Duplicate detector state codec for state type: " + stateType.getName());
            }
        }
        this.byAlgorithm = Map.copyOf(algorithms);
        this.byStateType = Map.copyOf(stateTypes);
    }

    @Override
    public Optional<DetectorStateCodec<?>> findByAlgorithm(String algorithm) {
        if (algorithm == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byAlgorithm.get(algorithm));
    }

    @Override
    public Optional<DetectorStateCodec<?>> findByState(DetectorState state) {
        if (state == null) {
            return Optional.empty();
        }
        DetectorStateCodec<?> exactMatch = byStateType.get(state.getClass());
        if (exactMatch != null) {
            return Optional.of(exactMatch);
        }
        return findByAlgorithm(state.algorithm());
    }

    @Override
    public Collection<String> algorithms() {
        return byAlgorithm.keySet();
    }
}
