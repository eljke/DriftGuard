package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;

import java.util.Optional;

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
}
