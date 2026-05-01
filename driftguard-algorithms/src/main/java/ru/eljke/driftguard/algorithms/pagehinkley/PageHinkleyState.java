package ru.eljke.driftguard.algorithms.pagehinkley;

import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Состояние detector-а Page-Hinkley для одного потока метрик.
 */
public record PageHinkleyState(
        long count,
        double mean,
        double cumulative,
        double minCumulative
) implements DetectorState {
    @Override
    public String algorithm() {
        return PageHinkleyConfig.ALGORITHM;
    }
}
