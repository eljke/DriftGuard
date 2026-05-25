package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.domain.MetricPoint;

/**
 * Contract implemented by streaming drift-detection algorithms.
 *
 * <p>Applications can provide their own algorithm by implementing this
 * interface and registering it in {@link DetectorRegistry}. Spring Boot users
 * can expose the implementation as a bean; the starter adds it to the registry
 * together with built-in algorithms.</p>
 */
public interface DetectorAlgorithm<C extends DetectorConfig, S extends DetectorState> {
    /**
     * Stable algorithm id used by {@link DetectorConfig#algorithm()}.
     */
    String name();

    /**
     * Runtime config type accepted by this algorithm.
     */
    Class<C> configType();

    /**
     * Creates the initial state for a new metric stream and detector definition.
     */
    S initialState(C config);

    /**
     * Processes one metric point and returns updated state plus an optional event.
     */
    DetectionResult<S> detect(MetricPoint point, S state, C config, DetectionContext context);
}


