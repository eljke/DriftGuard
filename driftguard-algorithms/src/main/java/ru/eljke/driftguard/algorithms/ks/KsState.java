package ru.eljke.driftguard.algorithms.ks;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Baseline and current samples used by the Kolmogorov-Smirnov detector.
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


