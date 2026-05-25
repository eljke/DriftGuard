package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry of detector state codecs available to an infrastructure adapter.
 */
public interface DetectorStateCodecRegistry {
    Optional<DetectorStateCodec<?>> findByAlgorithm(String algorithm);

    Optional<DetectorStateCodec<?>> findByState(DetectorState state);

    Collection<String> algorithms();
}

