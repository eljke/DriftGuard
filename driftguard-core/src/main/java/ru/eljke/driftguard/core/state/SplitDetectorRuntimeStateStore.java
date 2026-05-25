package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Compatibility adapter over the older split stores.
 *
 * <p>New code should prefer {@link DetectorRuntimeStateStore}. This
 * adapter exists so existing constructors and user-provided
 * implementations {@link DetectorStateStore}/{@link EmissionStateStore} continue to
 * work without an immediate migration.</p>
 */
public final class SplitDetectorRuntimeStateStore implements DetectorRuntimeStateStore {
    private final DetectorStateStore detectorStateStore;
    private final EmissionStateStore emissionStateStore;

    public SplitDetectorRuntimeStateStore(
            DetectorStateStore detectorStateStore,
            EmissionStateStore emissionStateStore
    ) {
        this.detectorStateStore = DriftGuardErrors.requireNonNull(detectorStateStore, "detectorStateStore");
        this.emissionStateStore = DriftGuardErrors.requireNonNull(emissionStateStore, "emissionStateStore");
    }

    @Override
    public Optional<DetectorRuntimeState> get(DetectorInstanceKey key) {
        Optional<DetectorState> detectorState = detectorStateStore.get(key);
        if (detectorState.isEmpty()) {
            return Optional.empty();
        }
        EmissionState emissionState = emissionStateStore.get(key).orElse(EmissionState.EMPTY);
        return Optional.of(new DetectorRuntimeState(detectorState.get(), emissionState, 0));
    }

    @Override
    public void put(DetectorInstanceKey key, DetectorRuntimeState state) {
        detectorStateStore.put(key, state.detectorState());
        emissionStateStore.put(key, state.emissionState());
    }

    @Override
    public DetectorRuntimeState update(
            DetectorInstanceKey key,
            Supplier<DetectorRuntimeState> initialState,
            UnaryOperator<DetectorRuntimeState> transition
    ) {
        DriftGuardErrors.requireNonNull(key, "key");
        DriftGuardErrors.requireNonNull(initialState, "initialState");
        DriftGuardErrors.requireNonNull(transition, "transition");
        synchronized (this) {
            DetectorRuntimeState current = get(key).orElseGet(initialState);
            DetectorRuntimeState updated = DriftGuardErrors.requireNonNull(transition.apply(current), "updatedRuntimeState");
            put(key, updated);
            return updated;
        }
    }
}


