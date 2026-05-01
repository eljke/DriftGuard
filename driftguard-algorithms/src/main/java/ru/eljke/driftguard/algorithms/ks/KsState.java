package ru.eljke.driftguard.algorithms.ks;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Baseline- и current-выборки, используемые detector-ом Колмогорова-Смирнова.
 */
public record KsState(
        SlidingDoubleWindow baseline,
        SlidingDoubleWindow current
) implements DetectorState {
    @Override
    public String algorithm() {
        return KsConfig.ALGORITHM;
    }
}
