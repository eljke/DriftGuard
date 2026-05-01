package ru.eljke.driftguard.core.detector;

/**
 * Snapshot состояния, принадлежащий алгоритму.
 *
 * <p>Core считает состояние detector-а opaque-объектом. Kafka-, Spring- или
 * file-based adapter-ы могут сериализовать реализации по-разному, но должны
 * сохранять значение {@link #algorithm()} для проверок совместимости.</p>
 */
public interface DetectorState {
    /**
     * Имя алгоритма, который создал это состояние.
     */
    String algorithm();
}
