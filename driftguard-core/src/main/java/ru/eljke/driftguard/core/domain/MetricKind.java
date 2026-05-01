package ru.eljke.driftguard.core.domain;

/**
 * Семантический тип значения метрики.
 *
 * <p>Тип помогает будущим adapter-ам и detector-ам корректно интерпретировать
 * значения без hardcode-а имён метрик.</p>
 */
public enum MetricKind {
    GAUGE,
    COUNTER,
    RATE,
    DURATION,
    SIZE,
    COUNT,
    PERCENTAGE
}
