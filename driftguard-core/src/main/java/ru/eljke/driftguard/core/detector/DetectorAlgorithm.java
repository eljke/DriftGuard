package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.domain.MetricPoint;

/**
 * Точка расширения для всех алгоритмов обнаружения drift-а.
 *
 * <p>Реализации должны быть детерминированными и не выполнять скрытых
 * side-effect-ов над состоянием detector-а: они получают предыдущее состояние
 * и возвращают новое состояние в {@link DetectionResult}. Инфраструктурные
 * модули решают, где это состояние физически хранится.</p>
 *
 * @param <C> типизированная конфигурация, принимаемая алгоритмом
 * @param <S> типизированное состояние, поддерживаемое алгоритмом
 */
public interface DetectorAlgorithm<C extends DetectorConfig, S extends DetectorState> {
    /**
     * Стабильное имя алгоритма, используемое в конфигурации и drift events.
     */
    String name();

    /**
     * Класс конфигурации, который engine использует для runtime-проверки типов.
     */
    Class<C> configType();

    /**
     * Создаёт начальное состояние для экземпляра detector-а.
     */
    S initialState(C config);

    /**
     * Обрабатывает одну точку метрики и возвращает обновлённое состояние и необязательное событие.
     */
    DetectionResult<S> detect(MetricPoint point, S state, C config, DetectionContext context);
}
