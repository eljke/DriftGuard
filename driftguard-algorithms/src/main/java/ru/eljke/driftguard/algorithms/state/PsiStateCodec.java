package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.psi.PsiConfig;
import ru.eljke.driftguard.algorithms.psi.PsiState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * English API documentation.
 */
public final class PsiStateCodec implements DetectorStateCodec<PsiState> {
    @Override
    public String algorithm() {
        return PsiConfig.ALGORITHM;
    }

    @Override
    public Class<PsiState> stateType() {
        return PsiState.class;
    }

    @Override
    public byte[] serialize(PsiState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            SlidingDoubleWindowCodec.write(output, state.baseline());
            SlidingDoubleWindowCodec.write(output, state.current());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize PSI state", e);
        }
    }

    @Override
    public PsiState deserialize(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return new PsiState(
                    SlidingDoubleWindowCodec.read(input),
                    SlidingDoubleWindowCodec.read(input)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot deserialize PSI state", e);
        }
    }
}


