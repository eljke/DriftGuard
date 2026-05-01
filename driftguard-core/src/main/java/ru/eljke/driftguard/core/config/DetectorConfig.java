package ru.eljke.driftguard.core.config;

/**
 * Маркерный контракт для типизированной конфигурации detector-а.
 *
 * <p>Каждый алгоритм должен предоставлять собственную immutable-реализацию,
 * чтобы некорректные пороги, окна или baseline-настройки можно было проверить
 * при создании объекта.</p>
 */
public interface DetectorConfig {
    /**
     * Имя алгоритма, по которому реализация ищется в detector registry.
     */
    String algorithm();
}
