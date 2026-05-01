package ru.eljke.driftguard.testkit;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Воспроизводимый сценарий генерации технических метрик.
 */
public interface MetricScenario {
    /**
     * Человекочитаемое имя сценария.
     */
    String name();

    /**
     * Генерирует поток точек метрик.
     */
    List<MetricPoint> generate();

    /**
     * Возвращает интервалы, в которых сценарий ожидает drift.
     */
    List<DriftInterval> expectedDrifts();
}
