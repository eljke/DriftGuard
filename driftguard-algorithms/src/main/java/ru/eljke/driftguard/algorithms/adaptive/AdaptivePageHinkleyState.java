package ru.eljke.driftguard.algorithms.adaptive;

import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyState;
import ru.eljke.driftguard.core.detector.DetectorState;

import java.util.List;

public record AdaptivePageHinkleyState(
        List<Double> baselineValues,
        DetectorSensitivityProfile selectedProfile,
        PageHinkleyState detectorState
) implements DetectorState {
    public AdaptivePageHinkleyState {
        baselineValues = List.copyOf(baselineValues);
    }

    public static AdaptivePageHinkleyState calibrating() {
        return new AdaptivePageHinkleyState(List.of(), null, null);
    }

    public boolean calibrated() {
        return selectedProfile != null;
    }

    @Override
    public String algorithm() {
        return AdaptivePageHinkleyConfig.ALGORITHM;
    }
}
