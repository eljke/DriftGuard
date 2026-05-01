package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.Objects;

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
        metricKey = Objects.requireNonNull(metricKey, "metricKey must not be null");
        if (detectorName == null || detectorName.isBlank()) {
            throw new IllegalArgumentException("detectorName must not be blank");
        }
        detectorName = detectorName.trim();
    }
}
