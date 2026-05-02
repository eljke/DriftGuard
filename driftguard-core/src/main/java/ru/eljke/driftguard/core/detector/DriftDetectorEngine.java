package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.CoreErrorReason;
import ru.eljke.driftguard.core.error.DriftGuardErrors;
import ru.eljke.driftguard.core.state.DetectorStateStore;
import ru.eljke.driftguard.core.state.EmissionStateStore;
import ru.eljke.driftguard.core.state.InMemoryEmissionStateStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transport-agnostic оркестратор обнаружения drift-а в метриках.
 *
 * <p>Engine не владеет потоками исполнения и не открывает сетевых соединений.
 * Он применяет настроенные detector definitions к входящим {@link MetricPoint},
 * находит алгоритмы через {@link DetectorRegistry}, читает и сохраняет
 * состояние через {@link DetectorStateStore}, а затем возвращает созданные
 * {@link DriftEvent}.</p>
 */
public final class DriftDetectorEngine {
    private final DetectorRegistry registry;
    private final DetectorStateStore stateStore;
    private final EmissionStateStore emissionStateStore;
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
        this.registry = DriftGuardErrors.requireNonNull(registry, "registry");
        this.stateStore = DriftGuardErrors.requireNonNull(stateStore, "stateStore");
        this.emissionStateStore = DriftGuardErrors.requireNonNull(emissionStateStore, "emissionStateStore");
        this.definitions = List.copyOf(definitions == null ? List.of() : definitions);
        this.listeners = List.copyOf(listeners == null ? List.of() : listeners);
    }

    /**
     * Обрабатывает одну точку метрики через все подходящие detector definitions.
     *
     * @return immutable-список drift events; обычно пустой или из одного элемента,
     * но может содержать несколько событий, если подошло несколько definitions
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
        AtomicReference<DetectionResult<S>> resultReference = new AtomicReference<>();
        stateStore.update(instanceKey, () -> algorithm.initialState(config), currentState -> {
            if (!currentState.algorithm().equals(algorithm.name())) {
                throw DriftGuardErrors.validation(CoreErrorReason.STATE_ALGORITHM_MISMATCH, instanceKey);
            }
            S typedState = castState(currentState);
            DetectionResult<S> result = algorithm.detect(point, typedState, config, new DetectionContext(definition.name()));
            resultReference.set(result);
            return result.state();
        });

        DetectionResult<S> result = resultReference.get();
        if (result.eventValue().isEmpty()) {
            return observeNormalPoint(point, instanceKey, definition);
        }
        return result.eventValue()
                .filter(event -> shouldEmit(instanceKey, definition, event))
                .stream()
                .toList();
    }

    private List<DriftEvent> observeNormalPoint(
            MetricPoint point,
            DetectorInstanceKey instanceKey,
            DetectorDefinition definition
    ) {
        AtomicReference<DriftEvent> recoveryEventReference = new AtomicReference<>();
        emissionStateStore.update(instanceKey, previous -> {
            if (previous.activeEpisode()) {
                int consecutiveNormal = previous.consecutiveNormal() + 1;
                boolean recovered = consecutiveNormal >= definition.emissionPolicy().recoveryConsecutiveNormal();
                DriftEvent recoveryEvent = recovered
                        ? previous.lastEmittedEventValue()
                        .map(event -> event.recoveredAt(point.timestamp(), definition.emissionPolicy().recoveryConsecutiveNormal()))
                        .orElse(null)
                        : null;
                recoveryEventReference.set(recoveryEvent);
                return new EmissionState(
                        recovered ? 0 : previous.consecutiveSignals(),
                        previous.lastEmittedAt(),
                        !recovered,
                        recovered ? 0 : consecutiveNormal,
                        recovered ? null : previous.lastEmittedEvent()
                );
            }
            if (previous.consecutiveSignals() > 0 || previous.consecutiveNormal() > 0) {
                return new EmissionState(0, previous.lastEmittedAt(), false, 0, previous.lastEmittedEvent());
            }
            return previous;
        });
        DriftEvent recoveryEvent = recoveryEventReference.get();
        return recoveryEvent == null ? List.of() : List.of(recoveryEvent);
    }

    private boolean shouldEmit(DetectorInstanceKey instanceKey, DetectorDefinition definition, DriftEvent event) {
        AtomicBoolean emitReference = new AtomicBoolean(false);
        emissionStateStore.update(instanceKey, previous -> {
            if (previous.activeEpisode()) {
                return new EmissionState(
                        previous.consecutiveSignals() + 1,
                        previous.lastEmittedAt(),
                        true,
                        0
                );
            }

            int consecutiveSignals = previous.consecutiveSignals() + 1;
            boolean enoughSignals = consecutiveSignals >= definition.emissionPolicy().minConsecutiveSignals();
            boolean cooldownElapsed = previous.lastEmittedAtValue()
                    .map(last -> !event.detectedAt().isBefore(last.plus(definition.emissionPolicy().cooldown())))
                    .orElse(true);
            boolean emit = enoughSignals && cooldownElapsed;
            emitReference.set(emit);
            return new EmissionState(
                    consecutiveSignals,
                    emit ? event.detectedAt() : previous.lastEmittedAt(),
                    emit,
                    0,
                    emit ? event : previous.lastEmittedEvent()
            );
        });
        return emitReference.get();
    }

    private void notifyCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        for (DriftDetectionListener listener : listeners) {
            try {
                listener.onDetectionCompleted(point, events, durationNanos);
            } catch (RuntimeException ignored) {
                // Listener-ошибка не должна ломать detection pipeline.
            }
        }
    }

    private void notifyFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
        for (DriftDetectionListener listener : listeners) {
            try {
                listener.onDetectionFailed(point, exception, durationNanos);
            } catch (RuntimeException ignored) {
                // Listener-ошибка не должна маскировать исходную ошибку detection-а.
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <S extends DetectorState> S castState(DetectorState state) {
        return (S) state;
    }
}
