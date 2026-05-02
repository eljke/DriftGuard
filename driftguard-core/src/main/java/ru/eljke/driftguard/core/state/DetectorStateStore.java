package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Абстракция хранилища состояний detector-ов.
 *
 * <p>Core предоставляет in-memory реализацию. Kafka Streams должен адаптировать
 * этот контракт к state store, а другие окружения могут использовать БД,
 * встроенные cache-и или временное хранилище для replay-сценариев.</p>
 */
public interface DetectorStateStore {
    /**
     * Читает состояние для пары metric/detector.
     */
    Optional<DetectorState> get(DetectorInstanceKey key);

    /**
     * Сохраняет последнее состояние для пары metric/detector.
     */
    void put(DetectorInstanceKey key, DetectorState state);

    /**
     * Атомарно читает, изменяет и сохраняет состояние detector-а.
     *
     * <p>Это основной контракт для runtime-кода. Простые реализации могут
     * наследовать synchronized fallback, а production-хранилища должны
     * переопределять метод нативной атомарной операцией: compute, transaction,
     * compare-and-set или state-store update.</p>
     */
    default DetectorState update(
            DetectorInstanceKey key,
            Supplier<? extends DetectorState> initialState,
            UnaryOperator<DetectorState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        synchronized (this) {
            DetectorState current = get(key).orElseGet(initialState);
            DetectorState updated = DriftGuardErrors.requireNonNull(transition.apply(current), "updatedState");
            put(key, updated);
            return updated;
        }
    }
}
