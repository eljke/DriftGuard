package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.adaptive.AdaptivePageHinkleyConfig;
import ru.eljke.driftguard.algorithms.adaptive.AdaptivePageHinkleyState;
import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public final class AdaptivePageHinkleyStateCodec implements DetectorStateCodec<AdaptivePageHinkleyState> {
    @Override
    public String algorithm() {
        return AdaptivePageHinkleyConfig.ALGORITHM;
    }

    @Override
    public Class<AdaptivePageHinkleyState> stateType() {
        return AdaptivePageHinkleyState.class;
    }

    @Override
    public byte[] serialize(AdaptivePageHinkleyState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(state.baselineValues().size());
            for (double value : state.baselineValues()) {
                output.writeDouble(value);
            }
            output.writeBoolean(state.calibrated());
            if (state.calibrated()) {
                output.writeUTF(state.selectedProfile().name());
                output.writeLong(state.detectorState().count());
                output.writeDouble(state.detectorState().mean());
                output.writeDouble(state.detectorState().cumulative());
                output.writeDouble(state.detectorState().minCumulative());
            }
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot serialize adaptive Page-Hinkley state", exception);
        }
    }

    @Override
    public AdaptivePageHinkleyState deserialize(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            int baselineSize = input.readInt();
            List<Double> baseline = new ArrayList<>(baselineSize);
            for (int index = 0; index < baselineSize; index++) {
                baseline.add(input.readDouble());
            }
            if (!input.readBoolean()) {
                return new AdaptivePageHinkleyState(baseline, null, null);
            }
            return new AdaptivePageHinkleyState(
                    List.of(),
                    DetectorSensitivityProfile.valueOf(input.readUTF()),
                    new PageHinkleyState(
                            input.readLong(),
                            input.readDouble(),
                            input.readDouble(),
                            input.readDouble()
                    )
            );
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot deserialize adaptive Page-Hinkley state", exception);
        }
    }
}
