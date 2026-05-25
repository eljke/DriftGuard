package ru.eljke.driftguard.algorithms.state;

import ru.eljke.driftguard.algorithms.adwin.AdwinConfig;
import ru.eljke.driftguard.algorithms.adwin.AdwinState;
import ru.eljke.driftguard.core.state.DetectorStateCodec;

/**
 * Codec for ADWIN detector state snapshots.
 */
public final class AdwinStateCodec implements DetectorStateCodec<AdwinState> {
    @Override
    public String algorithm() {
        return AdwinConfig.ALGORITHM;
    }

    @Override
    public Class<AdwinState> stateType() {
        return AdwinState.class;
    }

    @Override
    public byte[] serialize(AdwinState state) {
        return SlidingDoubleWindowCodec.serialize(state.window());
    }

    @Override
    public AdwinState deserialize(byte[] payload) {
        return new AdwinState(SlidingDoubleWindowCodec.deserialize(payload));
    }
}
