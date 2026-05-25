package ru.eljke.driftguard.core.domain;

/**
 * Semantic type of a metric value.
 *
 * <p>The type helps future adapters and detectors interpret
 * values without hardcoding metric names.</p>
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


