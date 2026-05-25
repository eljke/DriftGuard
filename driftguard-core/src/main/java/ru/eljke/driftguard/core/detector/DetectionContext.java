package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Metadata of one call that the engine passes to an algorithm.
 *
 * @param detectorName documented value
 */
public record DetectionContext(
        String detectorName
) {
    public DetectionContext {
        detectorName = DriftGuardErrors.requireNonBlank(detectorName, "detectorName");
    }
}


