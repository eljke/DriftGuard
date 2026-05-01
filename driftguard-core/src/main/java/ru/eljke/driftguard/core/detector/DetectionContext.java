package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Метаданные одного вызова, которые engine передаёт алгоритму.
 *
 * @param detectorName имя настроенного экземпляра detector-а, а не только имя алгоритма
 */
public record DetectionContext(
        String detectorName
) {
    public DetectionContext {
        detectorName = DriftGuardErrors.requireNonBlank(detectorName, "detectorName");
    }
}
