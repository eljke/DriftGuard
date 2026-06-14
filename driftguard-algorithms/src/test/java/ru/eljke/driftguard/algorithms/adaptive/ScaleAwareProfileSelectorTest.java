package ru.eljke.driftguard.algorithms.adaptive;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScaleAwareProfileSelectorTest {
    private final ScaleAwareProfileSelector selector = new ScaleAwareProfileSelector();

    @Test
    void selectsAggressiveForStableRate() {
        assertEquals(
                DetectorSensitivityProfile.AGGRESSIVE,
                selector.select(BaselineCharacteristics.from(List.of(
                        0.010, 0.011, 0.009, 0.010, 0.011, 0.010, 0.009, 0.010
                )))
        );
    }

    @Test
    void selectsBalancedForStableLatency() {
        assertEquals(
                DetectorSensitivityProfile.BALANCED,
                selector.select(BaselineCharacteristics.from(List.of(
                        99.0, 101.0, 100.0, 102.0, 98.0, 100.0, 101.0, 99.0
                )))
        );
    }

    @Test
    void selectsConservativeForVolatileStream() {
        assertEquals(
                DetectorSensitivityProfile.CONSERVATIVE,
                selector.select(BaselineCharacteristics.from(List.of(
                        50.0, 150.0, 45.0, 155.0, 40.0, 160.0, 35.0, 165.0
                )))
        );
    }
}
