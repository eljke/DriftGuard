package ru.eljke.driftguard.core.state;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectorRuntimeStateTest {
    @Test
    void initialStateUsesCurrentSchemaVersion() {
        DetectorRuntimeState state = DetectorRuntimeState.initial(new CountingState(0));

        assertEquals(DetectorRuntimeStateSchema.CURRENT_VERSION, state.schemaVersion());
        assertEquals(0, state.version());
    }

    @Test
    void restoreRejectsUnsupportedSchemaVersion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DetectorRuntimeState.restore(
                        DetectorRuntimeStateSchema.CURRENT_VERSION + 1,
                        new CountingState(0),
                        EmissionState.EMPTY,
                        0
                )
        );
    }

    @Test
    void runtimeVersionAdvancesIndependentlyFromSchemaVersion() {
        DetectorRuntimeState state = DetectorRuntimeState.initial(new CountingState(0))
                .advance(new CountingState(1), EmissionState.EMPTY);

        assertEquals(DetectorRuntimeStateSchema.CURRENT_VERSION, state.schemaVersion());
        assertEquals(1, state.version());
    }

    private record CountingState(int count) implements DetectorState {
        @Override
        public String algorithm() {
            return "counting";
        }
    }
}
