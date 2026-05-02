package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Listener жизненного цикла detection-а.
 *
 * <p>Core не зависит от Micrometer, logs или Spring. Внешние модули могут
 * подключать listener-ы для метрик, аудита или отладки, не меняя алгоритмы и
 * транспортно-независимую логику engine.</p>
 */
public interface DriftDetectionListener {
    /**
     * Вызывается после успешной обработки одной точки метрики.
     *
     * @param point обработанная точка метрики
     * @param events опубликованные drift events
     * @param durationNanos длительность обработки в наносекундах
     */
    default void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
    }

    /**
     * Вызывается, если detection завершился ошибкой.
     *
     * @param point точка метрики, на которой произошла ошибка
     * @param exception исходная ошибка detection pipeline
     * @param durationNanos длительность обработки до ошибки в наносекундах
     */
    default void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
    }
}