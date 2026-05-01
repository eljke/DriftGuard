package ru.eljke.driftguard.testkit;

import java.time.Duration;

/**
 * Метрики качества детекции для одного сценария.
 *
 * @param events всего созданных событий
 * @param truePositiveEvents событий внутри ожидаемых drift-интервалов
 * @param falsePositiveEvents событий вне ожидаемых drift-интервалов
 * @param detected был ли обнаружен хотя бы один ожидаемый drift-интервал
 * @param firstDetectionDelay задержка первого обнаружения относительно начала drift-а
 */
public record DetectionMetrics(
        int events,
        int truePositiveEvents,
        int falsePositiveEvents,
        boolean detected,
        Duration firstDetectionDelay
) {
}
