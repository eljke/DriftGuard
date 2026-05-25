package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.ks.KsConfig;
import ru.eljke.driftguard.algorithms.ks.KsState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * State codec for Kolmogorov-Smirnov detector samples.
 */
public final class KsStateCodec implements DetectorStateCodec<KsState> {
    @Override
    public String algorithm() {
        return KsConfig.ALGORITHM;
    }

    @Override
    public Class<KsState> stateType() {
        return KsState.class;
    }

    @Override
    public byte[] serialize(KsState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            SlidingDoubleWindowCodec.write(output, state.baseline());
            SlidingDoubleWindowCodec.write(output, state.current());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize KS state", e);
        }
    }

    @Override
    public KsState deserialize(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return new KsState(
                    SlidingDoubleWindowCodec.read(input),
                    SlidingDoubleWindowCodec.read(input)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot deserialize KS state", e);
        }
    }
}


