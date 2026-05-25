package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Key that isolates detector state by metric stream and detector definition.
 *
 * English API documentation.
 * English API documentation.
 */
public record DetectorInstanceKey(
        MetricKey metricKey,
        String detectorName
) {
    public DetectorInstanceKey {
        metricKey = DriftGuardErrors.requireNonNull(metricKey, "metricKey");
        detectorName = DriftGuardErrors.requireNonBlank(detectorName, "detectorName");
    }
}


