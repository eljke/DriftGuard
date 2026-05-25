package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Codec for serializing state snapshots of a concrete detector.
 *
 * <p>The core module deliberately does not choose JSON, Avro, Protobuf or another physical format.
 * Infrastructure adapters provide concrete codecs and use this SPI to persist opaque
 * {@link DetectorState} implementations without pulling adapter dependencies into
 * the domain model.</p>
 *
 * @param <S> concrete detector state type supported by this codec
 */
public interface DetectorStateCodec<S extends DetectorState> {
    /**
     * Stable algorithm name returned by {@link DetectorState#algorithm()}.
     */
    String algorithm();

    /**
     * Concrete state class handled by this codec.
     */
    Class<S> stateType();

    /**
     * Serializes a concrete detector state to adapter-format bytes.
     */
    byte[] serialize(S state);

    /**
     * Restores a concrete detector state from adapter-format bytes.
     */
    S deserialize(byte[] payload);

    /**
     * Runtime-safe serialization entry point for infrastructure adapters.
     */
    default byte[] serializeState(DetectorState state) {
        DriftGuardErrors.requireNonNull(state, "state");
        return serialize(stateType().cast(state));
    }
}


