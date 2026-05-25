package ru.eljke.driftguard.core.detector;

/**
 * State snapshot owned by an algorithm.
 *
 * <p>Core treats detector state as an opaque object. Kafka-, Spring- or
 * file-based adapters can serialize implementations differently, but must
 * preserve the value of {@link #algorithm()} for compatibility checks.</p>
 */
public interface DetectorState {
    /**
     * Algorithm name that created this state.
     */
    String algorithm();
}


