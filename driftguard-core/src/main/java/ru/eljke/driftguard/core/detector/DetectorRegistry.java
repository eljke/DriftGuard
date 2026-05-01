package ru.eljke.driftguard.core.detector;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry, который сопоставляет настроенные имена алгоритмов с реализациями.
 */
public interface DetectorRegistry {
    /**
     * Ищет зарегистрированный алгоритм по его стабильному имени.
     */
    Optional<DetectorAlgorithm<?, ?>> find(String name);

    /**
     * Возвращает имена всех зарегистрированных алгоритмов.
     */
    Collection<String> algorithmNames();
}
