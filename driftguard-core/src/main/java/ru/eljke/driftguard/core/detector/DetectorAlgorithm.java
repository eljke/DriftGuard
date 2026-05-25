package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.domain.MetricPoint;

/**
 * English API documentation.
 *
 * English API documentation.
 * English API documentation.
 * English API documentation.
 * English API documentation.
 *
 * English API documentation.
 * English API documentation.
 */
public interface DetectorAlgorithm<C extends DetectorConfig, S extends DetectorState> {
    /**
     * English API documentation.
     */
    String name();

    /**
     * English API documentation.
     */
    Class<C> configType();

    /**
     * English API documentation.
     */
    S initialState(C config);

    /**
     * Processes one metric point and returns updated state plus an optional event.
     */
    DetectionResult<S> detect(MetricPoint point, S state, C config, DetectionContext context);
}


