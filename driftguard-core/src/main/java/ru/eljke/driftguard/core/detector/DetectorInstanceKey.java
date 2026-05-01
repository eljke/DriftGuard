package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Ключ для изоляции состояния detector-а по потоку метрик и detector definition.
 *
 * <p>Одна и та же метрика может проверяться несколькими detector definitions,
 * каждая из которых имеет независимое состояние и пороги.</p>
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
