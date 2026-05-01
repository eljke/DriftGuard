package ru.eljke.driftguard.core.detector;

/**
 * Метаданные одного вызова, которые engine передаёт алгоритму.
 *
 * @param detectorName имя настроенного экземпляра detector-а, а не только имя алгоритма
 */
public record DetectionContext(
        String detectorName
) {
    public DetectionContext {
        if (detectorName == null || detectorName.isBlank()) {
            throw new IllegalArgumentException("detectorName must not be blank");
        }
    }
}
