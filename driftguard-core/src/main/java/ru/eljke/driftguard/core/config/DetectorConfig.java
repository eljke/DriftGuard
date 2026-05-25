package ru.eljke.driftguard.core.config;

/**
 * Marker contract for typed detector configuration.
 *
 * <p>Each algorithm should provide its own immutable implementation
 * so invalid thresholds, windows or baseline settings can be checked
 * when the object is created.</p>
 */
public interface DetectorConfig {
    /**
     * Algorithm name used to look up the implementation in a detector registry.
     */
    String algorithm();
}

