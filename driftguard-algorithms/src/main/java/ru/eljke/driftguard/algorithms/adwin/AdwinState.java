package ru.eljke.driftguard.algorithms.adwin;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Runtime state of the ADWIN detector for one metric stream.
 */
public record AdwinState(
        SlidingDoubleWindow window
) implements DetectorState {
    @Override
    public String algorithm() {
        return AdwinConfig.ALGORITHM;
    }
}
