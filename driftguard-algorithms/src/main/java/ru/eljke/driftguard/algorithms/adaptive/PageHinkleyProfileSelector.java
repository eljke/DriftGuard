package ru.eljke.driftguard.algorithms.adaptive;

@FunctionalInterface
public interface PageHinkleyProfileSelector {
    DetectorSensitivityProfile select(BaselineCharacteristics characteristics);
}
