package ru.eljke.driftguard.algorithms.pagehinkley;

import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Page-Hinkley detector state for one metric stream.
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


