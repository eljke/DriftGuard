package ru.eljke.driftguard.algorithms.chisquare;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Baseline- и current-выборки, используемые хи-квадрат detector-ом.
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
