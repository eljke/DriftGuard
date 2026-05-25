package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.support.SlidingDoubleWindow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * English API documentation.
 */
final class SlidingDoubleWindowCodec {
    private SlidingDoubleWindowCodec() {
    }

    static byte[] serialize(SlidingDoubleWindow window) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            write(output, window);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize sliding window", e);
        }
    }

    static SlidingDoubleWindow deserialize(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            return read(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot deserialize sliding window", e);
        }
    }

    static void write(DataOutputStream output, SlidingDoubleWindow window) throws IOException {
        double[] values = window.toArray();
        output.writeInt(window.capacity());
        output.writeInt(values.length);
        for (double value : values) {
            output.writeDouble(value);
        }
    }

    static SlidingDoubleWindow read(DataInputStream input) throws IOException {
        int capacity = input.readInt();
        int size = input.readInt();
        double[] values = new double[size];
        for (int i = 0; i < size; i++) {
            values[i] = input.readDouble();
        }
        return SlidingDoubleWindow.of(capacity, values);
    }
}


