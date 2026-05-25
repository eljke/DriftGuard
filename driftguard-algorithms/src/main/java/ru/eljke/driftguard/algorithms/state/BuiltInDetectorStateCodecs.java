package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.core.state.DetectorStateCodec;
import ru.eljke.driftguard.core.state.SimpleDetectorStateCodecRegistry;

import java.util.List;

/**
 * English API documentation.
 */
public final class BuiltInDetectorStateCodecs {
    private BuiltInDetectorStateCodecs() {
    }

    public static List<DetectorStateCodec<?>> all() {
        return List.of(
                new PageHinkleyStateCodec(),
                new AdwinStateCodec(),
                new PsiStateCodec(),
                new KsStateCodec(),
                new ChiSquareStateCodec()
        );
    }

    public static SimpleDetectorStateCodecRegistry registry() {
        return new SimpleDetectorStateCodecRegistry(all());
    }
}


