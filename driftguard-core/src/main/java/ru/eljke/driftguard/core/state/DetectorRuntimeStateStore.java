package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Хранилище полного runtime state для одного detector instance.
 *
 * <p>Это основной state-контракт для processing pipeline. Адаптеры Kafka,
 * JDBC или Redis должны сохранять {@link DetectorRuntimeState} атомарно, чтобы
 * detector state и emission state не расходились между собой.</p>
 */
public interface DetectorRuntimeStateStore {
    Optional<DetectorRuntimeState> get(DetectorInstanceKey key);

    void put(DetectorInstanceKey key, DetectorRuntimeState state);

    /**
     * Атомарно читает, изменяет и сохраняет полный runtime snapshot.
     */
    default DetectorRuntimeState update(
            DetectorInstanceKey key,
            Supplier<DetectorRuntimeState> initialState,
            UnaryOperator<DetectorRuntimeState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        synchronized (this) {
            DetectorRuntimeState current = get(key).orElseGet(initialState);
            DetectorRuntimeState updated = DriftGuardErrors.requireNonNull(transition.apply(current), "updatedRuntimeState");
            put(key, updated);
            return updated;
        }
    }
}
