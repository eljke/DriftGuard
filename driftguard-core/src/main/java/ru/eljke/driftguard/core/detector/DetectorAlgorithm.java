package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.domain.MetricPoint;

/**
 * Contract implemented by streaming drift-detection algorithms.
 *
 * Contract implemented by streaming drift-detection algorithms.
 * Contract implemented by streaming drift-detection algorithms.
 * Contract implemented by streaming drift-detection algorithms.
 * Contract implemented by streaming drift-detection algorithms.
 *
 * Contract implemented by streaming drift-detection algorithms.
 * Contract implemented by streaming drift-detection algorithms.
 */
public interface DetectorAlgorithm<C extends DetectorConfig, S extends DetectorState> {
    /**
     * Contract implemented by streaming drift-detection algorithms.
     */
    String name();

    /**
     * Contract implemented by streaming drift-detection algorithms.
     */
    Class<C> configType();

    /**
     * Contract implemented by streaming drift-detection algorithms.
     */
    S initialState(C config);

    /**
     * Processes one metric point and returns updated state plus an optional event.
     */
    DetectionResult<S> detect(MetricPoint point, S state, C config, DetectionContext context);
}


