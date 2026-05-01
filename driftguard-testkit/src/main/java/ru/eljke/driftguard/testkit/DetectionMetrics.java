package ru.eljke.driftguard.testkit;

import java.time.Duration;

/**
 * Метрики качества детекции для одного сценария.
 *
 * @param events всего созданных событий
 * @param truePositiveEvents событий внутри ожидаемых drift-интервалов
 * @param falsePositiveEvents событий вне ожидаемых drift-интервалов
 * @param expectedDriftIntervals число ожидаемых drift-интервалов
 * @param detectedDriftIntervals число ожидаемых интервалов, где было хотя бы одно событие
 * @param missedDriftIntervals число пропущенных ожидаемых интервалов
 * @param detected был ли обнаружен хотя бы один ожидаемый drift-интервал
 * @param firstDetectionDelay задержка первого обнаружения относительно начала drift-а
 * @param precision доля событий, попавших в ожидаемые drift-интервалы
 * @param recall доля ожидаемых drift-интервалов, в которых был сигнал
 */
public record DetectionMetrics(
        int events,
        int truePositiveEvents,
        int falsePositiveEvents,
        int expectedDriftIntervals,
        int detectedDriftIntervals,
        int missedDriftIntervals,
        boolean detected,
        Duration firstDetectionDelay,
        double precision,
        double recall
) {
}
