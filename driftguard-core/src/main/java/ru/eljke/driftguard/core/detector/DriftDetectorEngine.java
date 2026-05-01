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

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorStateStore stateStore,
            List<DetectorDefinition> definitions
    ) {
        this(registry, stateStore, new InMemoryEmissionStateStore(), definitions);
    }

    public DriftDetectorEngine(
            DetectorRegistry registry,
            DetectorStateStore stateStore,
            EmissionStateStore emissionStateStore,
            List<DetectorDefinition> definitions
    ) {
        this.registry = DriftGuardErrors.requireNonNull(registry, "registry");
        this.stateStore = DriftGuardErrors.requireNonNull(stateStore, "stateStore");
        this.emissionStateStore = DriftGuardErrors.requireNonNull(emissionStateStore, "emissionStateStore");
        this.definitions = List.copyOf(definitions == null ? List.of() : definitions);
    }

    /**
     * Обрабатывает одну точку метрики через все подходящие detector definitions.
     *
     * @return immutable-список drift events; обычно пустой или из одного элемента,
     * но может содержать несколько событий, если подошло несколько definitions
     */
    public List<DriftEvent> detect(MetricPoint point) {
        DriftGuardErrors.requireNonNull(point, "point");
        List<DriftEvent> events = new ArrayList<>();
        for (DetectorDefinition definition : definitions) {
            if (!definition.matches(point.key())) {
                continue;
            }
            events.addAll(detectWithDefinition(point, definition));
        }
        return List.copyOf(events);
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
        S state;
        DetectorState existing = stateStore.get(instanceKey).orElse(null);
        if (existing == null) {
            state = algorithm.initialState(config);
        } else {
            if (!existing.algorithm().equals(algorithm.name())) {
                throw DriftGuardErrors.validation(CoreErrorReason.STATE_ALGORITHM_MISMATCH, instanceKey);
            }
            state = castState(existing);
        }

        DetectionResult<S> result = algorithm.detect(point, state, config, new DetectionContext(definition.name()));
        stateStore.put(instanceKey, result.state());
        if (result.eventValue().isEmpty()) {
            resetConsecutiveSignals(instanceKey);
            return List.of();
        }
        return result.eventValue()
                .filter(event -> shouldEmit(instanceKey, definition, event))
                .stream()
                .toList();
    }

    private void resetConsecutiveSignals(DetectorInstanceKey instanceKey) {
        EmissionState previous = emissionStateStore.get(instanceKey).orElse(EmissionState.EMPTY);
        if (previous.consecutiveSignals() > 0) {
            emissionStateStore.put(instanceKey, new EmissionState(0, previous.lastEmittedAt()));
        }
    }

    private boolean shouldEmit(DetectorInstanceKey instanceKey, DetectorDefinition definition, DriftEvent event) {
        EmissionState previous = emissionStateStore.get(instanceKey).orElse(EmissionState.EMPTY);
        int consecutiveSignals = previous.consecutiveSignals() + 1;
        boolean enoughSignals = consecutiveSignals >= definition.emissionPolicy().minConsecutiveSignals();
        boolean cooldownElapsed = previous.lastEmittedAtValue()
                .map(last -> !event.detectedAt().isBefore(last.plus(definition.emissionPolicy().cooldown())))
                .orElse(true);
        boolean emit = enoughSignals && cooldownElapsed;
        emissionStateStore.put(instanceKey, new EmissionState(
                consecutiveSignals,
                emit ? event.detectedAt() : previous.lastEmittedAt()
        ));
        return emit;
    }

    @SuppressWarnings("unchecked")
    private static <S extends DetectorState> S castState(DetectorState state) {
        return (S) state;
    }
}
