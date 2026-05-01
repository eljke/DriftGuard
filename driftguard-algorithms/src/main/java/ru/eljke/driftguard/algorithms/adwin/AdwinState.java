package ru.eljke.driftguard.algorithms.adwin;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;

/**
 * Состояние ADWIN-style detector-а для одного потока метрик.
 */
public record AdwinState(
        SlidingDoubleWindow window
) implements DetectorState {
    @Override
    public String algorithm() {
        return AdwinConfig.ALGORITHM;
    }
}
