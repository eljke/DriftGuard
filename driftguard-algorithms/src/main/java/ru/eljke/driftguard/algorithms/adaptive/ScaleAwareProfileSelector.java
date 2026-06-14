package ru.eljke.driftguard.algorithms.adaptive;

public final class ScaleAwareProfileSelector implements PageHinkleyProfileSelector {
    @Override
    public DetectorSensitivityProfile select(BaselineCharacteristics characteristics) {
        if (Math.abs(characteristics.mean()) < 1.0
                && characteristics.coefficientOfVariation() < 0.5) {
            return DetectorSensitivityProfile.AGGRESSIVE;
        }
        if (Math.abs(characteristics.lagOneAutocorrelation()) >= 0.65
                || characteristics.coefficientOfVariation() >= 0.12) {
            return DetectorSensitivityProfile.CONSERVATIVE;
        }
        return DetectorSensitivityProfile.BALANCED;
    }
}
