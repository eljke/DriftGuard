package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.CoreErrorReason;
import ru.eljke.driftguard.core.error.DriftGuardErrors;
import ru.eljke.driftguard.core.state.DetectorRuntimeState;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateStore;
import ru.eljke.driftguard.core.state.DetectorStateStore;
import ru.eljke.driftguard.core.state.EmissionStateStore;
import ru.eljke.driftguard.core.state.InMemoryEmissionStateStore;
import ru.eljke.driftguard.core.state.SplitDetectorRuntimeStateStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transport-agnostic orchestrator for drift detection in metrics.
 *
 * <p>The engine does not own execution threads and does not open network connections.
 * It applies configured detector definitions to incoming {@link MetricPoint},
 * finds algorithms through {@link DetectorRegistry}, reads and saves
 * state through {@link DetectorStateStore}, and then returns created
 * {@link DriftEvent}.</p>
 */
public final class DriftDetectorEngine {
    private final DetectorRegistry registry;
    private final DetectorRuntimeStateStore runtimeStateStore;
    private final List<DetectorDefinition> definitions;
    private final List<DriftDetectionListener> listeners;

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorStateStore stateStore,
            List<DetectorDefinition> definitions
    ) {
        this(registry, stateStore, new InMemoryEmissionStateStore(), definitions, List.of());
    }

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorStateStore stateStore,
            EmissionStateStore emissionStateStore,
            List<DetectorDefinition> definitions
    ) {
        this(registry, stateStore, emissionStateStore, definitions, List.of());
    }

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorStateStore stateStore,
            EmissionStateStore emissionStateStore,
            List<DetectorDefinition> definitions,
            List<DriftDetectionListener> listeners
    ) {
        this(
                registry,
                new SplitDetectorRuntimeStateStore(stateStore, emissionStateStore),
                definitions,
                listeners
        );
    }

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorRuntimeStateStore runtimeStateStore,
            List<DetectorDefinition> definitions
    ) {
        this(registry, runtimeStateStore, definitions, List.of());
    }

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorRuntimeStateStore runtimeStateStore,
            List<DetectorDefinition> definitions,
            List<DriftDetectionListener> listeners
    ) {
        this.registry = DriftGuardErrors.requireNonNull(registry, "registry");
        this.runtimeStateStore = DriftGuardErrors.requireNonNull(runtimeStateStore, "runtimeStateStore");
        this.definitions = List.copyOf(definitions == null ? List.of() : definitions);
        this.listeners = List.copyOf(listeners == null ? List.of() : listeners);
    }

    /**
     * Processes one metric point through all matching detector definitions.
     *
     * @return immutable list of drift events; usually empty or containing one element,
     * but it can contain multiple events when several definitions match
     */
    public List<DriftEvent> detect(MetricPoint point) {
        DriftGuardErrors.requireNonNull(point, "point");
        long startedNanos = System.nanoTime();
        try {
            List<DriftEvent> events = new ArrayList<>();
            for (DetectorDefinition definition : definitions) {
                if (!definition.matches(point.key())) {
                    continue;
                }
                events.addAll(detectWithDefinition(point, definition));
            }
            List<DriftEvent> result = List.copyOf(events);
            notifyCompleted(point, result, System.nanoTime() - startedNanos);
            return result;
        } catch (RuntimeException exception) {
            notifyFailed(point, exception, System.nanoTime() - startedNanos);
            throw exception;
        }
    }

    private List<DriftEvent> detectWithDefinition(MetricPoint point, DetectorDefinition definition) {
        DetectorAlgorithm<?, ?> rawAlgorithm = registry.find(definition.config().algorithm())
                .orElseThrow(() -> DriftGuardErrors.validation(CoreErrorReason.UNKNOWN_ALGORITHM, definition.config().algorithm()));
        return detectTyped(point, definition, rawAlgorithm);
    }

    private <C extends DetectorConfig, S extends DetectorState> List<DriftEvent> detectTyped(
            MetricPoint point,
            DetectorDefinition definition,
            DetectorAlgorithm<C, S> algorithm
    ) {
        C config = algorithm.configType().cast(definition.config());
        DetectorInstanceKey instanceKey = new DetectorInstanceKey(point.key(), definition.name());
        AtomicReference<List<DriftEvent>> eventsReference = new AtomicReference<>(List.of());
        runtimeStateStore.update(
                instanceKey,
                () -> DetectorRuntimeState.initial(algorithm.initialState(config)),
                currentRuntimeState -> {
                    DetectorState currentDetectorState = currentRuntimeState.detectorState();
                    if (!currentDetectorState.algorithm().equals(algorithm.name())) {
                        throw DriftGuardErrors.validation(CoreErrorReason.STATE_ALGORITHM_MISMATCH, instanceKey);
                    }

                    S typedState = castState(currentDetectorState);
                    DetectionResult<S> result = algorithm.detect(
                            point,
                            typedState,
                            config,
                            new DetectionContext(definition.name())
                    );

                    EmissionTransition emissionTransition = result.eventValue()
                            .map(event -> onDriftSignal(currentRuntimeState.emissionState(), definition, event))
                            .orElseGet(() -> onNormalSignal(point, currentRuntimeState.emissionState(), definition));
                    eventsReference.set(emissionTransition.events());
                    return currentRuntimeState.advance(result.state(), emissionTransition.state());
                }
        );
        return eventsReference.get();
    }

    private EmissionTransition onNormalSignal(
            MetricPoint point,
            EmissionState previous,
            DetectorDefinition definition
    ) {
        if (previous.activeEpisode()) {
            DriftEvent lastEvent = previous.lastEmittedEventValue().orElse(null);
            if (!isBackNearBaseline(point, lastEvent)) {
                return new EmissionTransition(
                        new EmissionState(previous.consecutiveSignals(), previous.lastEmittedAt(), true, 0, previous.lastEmittedEvent()),
                        List.of()
                );
            }
            int consecutiveNormal = previous.consecutiveNormal() + 1;
            boolean recovered = consecutiveNormal >= definition.emissionPolicy().recoveryConsecutiveNormal();
            DriftEvent recoveryEvent = recovered
                    ? lastEvent.recoveredAt(point.timestamp(), definition.emissionPolicy().recoveryConsecutiveNormal(), point.value())
                    : null;
            EmissionState next = new EmissionState(
                    recovered ? 0 : previous.consecutiveSignals(),
                    previous.lastEmittedAt(),
                    !recovered,
                    recovered ? 0 : consecutiveNormal,
                    recovered ? null : previous.lastEmittedEvent()
            );
            return new EmissionTransition(next, recoveryEvent == null ? List.of() : List.of(recoveryEvent));
        }
        if (previous.consecutiveSignals() > 0 || previous.consecutiveNormal() > 0) {
            return new EmissionTransition(
                    new EmissionState(0, previous.lastEmittedAt(), false, 0, previous.lastEmittedEvent()),
                    List.of()
            );
        }
        return new EmissionTransition(previous, List.of());
    }

    private EmissionTransition onDriftSignal(EmissionState previous, DetectorDefinition definition, DriftEvent event) {
        if (previous.activeEpisode()) {
            return onActiveEpisodeSignal(previous, definition, event);
        }

        int consecutiveSignals = previous.consecutiveSignals() + 1;
        boolean enoughSignals = consecutiveSignals >= definition.emissionPolicy().minConsecutiveSignals();
        boolean cooldownElapsed = cooldownElapsed(previous, definition, event);
        boolean emit = enoughSignals && cooldownElapsed;
        EmissionState next = new EmissionState(
                consecutiveSignals,
                emit ? event.detectedAt() : previous.lastEmittedAt(),
                emit,
                0,
                emit ? event : previous.lastEmittedEvent()
        );
        return new EmissionTransition(next, emit ? List.of(event) : List.of());
    }

    private EmissionTransition onActiveEpisodeSignal(EmissionState previous, DetectorDefinition definition, DriftEvent event) {
        int consecutiveSignals = previous.consecutiveSignals() + 1;
        DriftEvent lastEvent = previous.lastEmittedEventValue().orElse(null);
        if (isOppositeDirection(lastEvent, event)) {
            if (!isBackNearBaseline(event, lastEvent)) {
                return new EmissionTransition(
                        new EmissionState(consecutiveSignals, previous.lastEmittedAt(), true, 0, previous.lastEmittedEvent()),
                        List.of()
                );
            }
            DriftEvent recovered = lastEvent.recoveredAt(event.detectedAt(), definition.emissionPolicy().recoveryConsecutiveNormal(), event.currentValue());
            return new EmissionTransition(
                    new EmissionState(0, event.detectedAt(), false, 0, recovered),
                    List.of(recovered)
            );
        }

        boolean emitOngoing = cooldownElapsed(previous, definition, event);
        DriftEvent ongoing = emitOngoing ? event.ongoingAt(event.detectedAt(), consecutiveSignals, lastEvent.baselineValue()) : null;
        EmissionState next = new EmissionState(
                consecutiveSignals,
                emitOngoing ? event.detectedAt() : previous.lastEmittedAt(),
                true,
                0,
                emitOngoing ? ongoing : previous.lastEmittedEvent()
        );
        return new EmissionTransition(next, ongoing == null ? List.of() : List.of(ongoing));
    }

    private static boolean isOppositeDirection(DriftEvent previous, DriftEvent current) {
        if (previous == null) {
            return false;
        }
        return (previous.direction() == DriftDirection.UP && current.direction() == DriftDirection.DOWN)
                || (previous.direction() == DriftDirection.DOWN && current.direction() == DriftDirection.UP);
    }

    private static boolean isBackNearBaseline(MetricPoint point, DriftEvent previous) {
        return previous == null || isBackNearBaseline(point.value(), previous);
    }

    private static boolean isBackNearBaseline(DriftEvent current, DriftEvent previous) {
        return previous == null || isBackNearBaseline(current.currentValue(), previous);
    }

    private static boolean isBackNearBaseline(double value, DriftEvent previous) {
        if (previous == null) {
            return true;
        }
        double baseline = previous.baselineValue();
        double driftValue = previous.currentValue();
        double tolerance = Math.abs(driftValue - baseline) * 0.25;
        return switch (previous.direction()) {
            case UP -> value <= baseline + tolerance;
            case DOWN -> value >= baseline - tolerance;
            case BOTH, DISTRIBUTION, UNKNOWN -> true;
        };
    }

    private static boolean cooldownElapsed(EmissionState previous, DetectorDefinition definition, DriftEvent event) {
        return previous.lastEmittedAtValue()
                .map(last -> !event.detectedAt().isBefore(last.plus(definition.emissionPolicy().cooldown())))
                .orElse(true);
    }

    private record EmissionTransition(EmissionState state, List<DriftEvent> events) {
    }

    private void notifyCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        for (DriftDetectionListener listener : listeners) {
            try {
                listener.onDetectionCompleted(point, events, durationNanos);
            } catch (RuntimeException ignored) {
                // Listener errors must not break the detection pipeline.
            }
        }
    }

    private void notifyFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
        for (DriftDetectionListener listener : listeners) {
            try {
                listener.onDetectionFailed(point, exception, durationNanos);
            } catch (RuntimeException ignored) {
                // Listener errors must not hide the original detection error.
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <S extends DetectorState> S castState(DetectorState state) {
        return (S) state;
    }
}

