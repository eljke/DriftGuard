package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.chisquare.ChiSquareConfig;
import ru.eljke.driftguard.algorithms.chisquare.ChiSquareState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Codec for chi-square detector state.
 */
public final class ChiSquareStateCodec implements DetectorStateCodec<ChiSquareState> {
    @Override
    public String algorithm() {
        return ChiSquareConfig.ALGORITHM;
    }

    @Override
    public Class<ChiSquareState> stateType() {
        return ChiSquareState.class;
    }

    @Override
    public byte[] serialize(ChiSquareState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            SlidingDoubleWindowCodec.write(output, state.baseline());
            SlidingDoubleWindowCodec.write(output, state.current());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize chi-square state", e);
        }
    }

    @Override
    public ChiSquareState deserialize(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return new ChiSquareState(
                    SlidingDoubleWindowCodec.read(input),
                    SlidingDoubleWindowCodec.read(input)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot deserialize chi-square state", e);
        }
    }
}


