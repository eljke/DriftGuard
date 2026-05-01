package ru.eljke.driftguard.algorithms.psi;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Baseline- и current-окна, используемые PSI detector-ом.
 */
public record PsiState(
        SlidingDoubleWindow baseline,
        SlidingDoubleWindow current
) implements DetectorState {
    @Override
    public String algorithm() {
        return PsiConfig.ALGORITHM;
    }
}
