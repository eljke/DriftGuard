package ru.eljke.driftguard.algorithms.chisquare;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Baseline and current samples used by the chi-square detector.
 */
public record ChiSquareState(
        SlidingDoubleWindow baseline,
        SlidingDoubleWindow current
) implements DetectorState {
    @Override
    public String algorithm() {
        return ChiSquareConfig.ALGORITHM;
    }
}


