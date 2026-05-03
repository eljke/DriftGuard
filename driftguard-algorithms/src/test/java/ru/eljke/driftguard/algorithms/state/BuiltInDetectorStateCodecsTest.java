package ru.eljke.driftguard.algorithms.state;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.adwin.AdwinState;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareConfig;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareState;
import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.ks.KsState;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyState;
import ru.eljke.driftguard.algorithms.psi.PsiConfig;
import ru.eljke.driftguard.algorithms.psi.PsiState;
import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;
import ru.eljke.driftguard.core.state.DetectorStateCodecRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInDetectorStateCodecsTest {
    @Test
    void registersAllBuiltInCodecs() {
        DetectorStateCodecRegistry registry = BuiltInDetectorStateCodecs.registry();

        assertTrue(registry.findByAlgorithm(PageHinkleyConfig.ALGORITHM).isPresent());
        assertTrue(registry.findByAlgorithm(AdwinConfig.ALGORITHM).isPresent());
        assertTrue(registry.findByAlgorithm(PsiConfig.ALGORITHM).isPresent());
        assertTrue(registry.findByAlgorithm(KsConfig.ALGORITHM).isPresent());
        assertTrue(registry.findByAlgorithm(ChiSquareConfig.ALGORITHM).isPresent());
        assertEquals(
                List.of("page-hinkley", "adwin", "psi", "ks", "chi-square"),
                List.copyOf(registry.algorithms())
        );
    }

    @Test
    void roundTripsPageHinkleyState() {
        PageHinkleyState state = new PageHinkleyState(42, 12.5, 3.25, -1.5);

        DetectorState restored = roundTrip(new PageHinkleyStateCodec(), state);

        assertEquals(state, restored);
    }

    @Test
    void roundTripsAdwinState() {
        AdwinState state = new AdwinState(window(5, 1.0, 2.0, 3.0));

        DetectorState restored = roundTrip(new AdwinStateCodec(), state);

        AdwinState restoredState = assertInstanceOf(AdwinState.class, restored);
        assertWindowEquals(state.window(), restoredState.window());
    }

    @Test
    void roundTripsPsiState() {
        PsiState state = new PsiState(
                window(4, 1.0, 2.0),
                window(3, 7.0, 8.0, 9.0)
        );

        DetectorState restored = roundTrip(new PsiStateCodec(), state);

        PsiState restoredState = assertInstanceOf(PsiState.class, restored);
        assertWindowEquals(state.baseline(), restoredState.baseline());
        assertWindowEquals(state.current(), restoredState.current());
    }

    @Test
    void roundTripsKsState() {
        KsState state = new KsState(
                window(4, 1.0, 2.0),
                window(3, 7.0, 8.0, 9.0)
        );

        DetectorState restored = roundTrip(new KsStateCodec(), state);

        KsState restoredState = assertInstanceOf(KsState.class, restored);
        assertWindowEquals(state.baseline(), restoredState.baseline());
        assertWindowEquals(state.current(), restoredState.current());
    }

    @Test
    void roundTripsChiSquareState() {
        ChiSquareState state = new ChiSquareState(
                window(4, 1.0, 2.0),
                window(3, 7.0, 8.0, 9.0)
        );

        DetectorState restored = roundTrip(new ChiSquareStateCodec(), state);

        ChiSquareState restoredState = assertInstanceOf(ChiSquareState.class, restored);
        assertWindowEquals(state.baseline(), restoredState.baseline());
        assertWindowEquals(state.current(), restoredState.current());
    }

    @Test
    void registryFindsCodecByRestoredState() {
        DetectorStateCodecRegistry registry = BuiltInDetectorStateCodecs.registry();
        DetectorState state = new PageHinkleyState(1, 2.0, 3.0, 4.0);

        assertTrue(registry.findByState(state).isPresent());
        assertFalse(registry.findByAlgorithm("unknown").isPresent());
    }

    private static <S extends DetectorState> DetectorState roundTrip(DetectorStateCodec<S> codec, S state) {
        return codec.deserialize(codec.serialize(state));
    }

    private static SlidingDoubleWindow window(int capacity, double... values) {
        return SlidingDoubleWindow.of(capacity, values);
    }

    private static void assertWindowEquals(SlidingDoubleWindow expected, SlidingDoubleWindow actual) {
        assertEquals(expected.capacity(), actual.capacity());
        assertArrayEquals(expected.toArray(), actual.toArray());
    }
}
