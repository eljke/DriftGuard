package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;

import java.util.Collection;
import java.util.Optional;

/**
 * Реестр кодеков состояния detector-ов, доступных инфраструктурному адаптеру.
 */
public interface DetectorStateCodecRegistry {
    Optional<DetectorStateCodec<?>> findByAlgorithm(String algorithm);

    Optional<DetectorStateCodec<?>> findByState(DetectorState state);

    Collection<String> algorithms();
}
