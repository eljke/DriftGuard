package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Codec for Page-Hinkley detector state.
 */
public final class PageHinkleyStateCodec implements DetectorStateCodec<PageHinkleyState> {
    @Override
    public String algorithm() {
        return PageHinkleyConfig.ALGORITHM;
    }

    @Override
    public Class<PageHinkleyState> stateType() {
        return PageHinkleyState.class;
    }

    @Override
    public byte[] serialize(PageHinkleyState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeLong(state.count());
            output.writeDouble(state.mean());
            output.writeDouble(state.cumulative());
            output.writeDouble(state.minCumulative());
            output.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize Page-Hinkley state", e);
        }
    }

    @Override
    public PageHinkleyState deserialize(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return new PageHinkleyState(
                    input.readLong(),
                    input.readDouble(),
                    input.readDouble(),
                    input.readDouble()
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot deserialize Page-Hinkley state", e);
        }
    }
}


