package ru.eljke.driftguard.core;

import lombok.Builder;
import lombok.Singular;
import ru.eljke.driftguard.core.adapter.MetricPointPublisher;
import ru.eljke.driftguard.core.alert.DriftAlertListener;
import ru.eljke.driftguard.core.alert.DriftAlertMapper;
import ru.eljke.driftguard.core.alert.DriftAlertSink;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.DriftDetectionListener;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.detector.DriftEventSinkListener;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.DriftGuardErrors;
import ru.eljke.driftguard.core.sink.DriftEventSink;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateStore;
import ru.eljke.driftguard.core.state.InMemoryDetectorRuntimeStateStore;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level facade for embedding DriftGuard in an application without binding
 * the application code to the lower-level engine wiring.
 */
public final class DriftGuard implements MetricPointPublisher {
    private final DriftDetectorEngine engine;
    private final List<DetectorDefinition> definitions;

    @Builder
    private DriftGuard(
            DetectorRegistry registry,
            DetectorRuntimeStateStore runtimeStateStore,
            @Singular("definition") List<DetectorDefinition> definitions,
            @Singular("listener") List<DriftDetectionListener> listeners,
            @Singular("sink") List<DriftEventSink> sinks,
            @Singular("alertSink") List<DriftAlertSink> alertSinks,
            DriftAlertMapper alertMapper
    ) {
        DetectorRegistry checkedRegistry = DriftGuardErrors.requireNonNull(registry, "registry");
        DetectorRuntimeStateStore checkedStateStore = runtimeStateStore == null
                ? new InMemoryDetectorRuntimeStateStore()
                : runtimeStateStore;
        this.definitions = List.copyOf(definitions == null ? List.of() : definitions);
        this.engine = new DriftDetectorEngine(
                checkedRegistry,
                checkedStateStore,
                this.definitions,
                effectiveListeners(listeners, sinks, alertSinks, alertMapper)
        );
    }

    @Override
    public List<DriftEvent> publish(MetricPoint point) {
        return detect(point);
    }

    public List<DriftEvent> detect(MetricPoint point) {
        return engine.detect(point);
    }

    public DriftDetectorEngine engine() {
        return engine;
    }

    public List<DetectorDefinition> definitions() {
        return definitions;
    }

    private static List<DriftDetectionListener> effectiveListeners(
            List<DriftDetectionListener> listeners,
            List<DriftEventSink> sinks,
            List<DriftAlertSink> alertSinks,
            DriftAlertMapper alertMapper
    ) {
        List<DriftDetectionListener> result = new ArrayList<>(listeners == null ? List.of() : listeners);
        if (sinks != null && !sinks.isEmpty()) {
            result.add(new DriftEventSinkListener(sinks));
        }
        if (alertSinks != null && !alertSinks.isEmpty()) {
            result.add(new DriftAlertListener(alertMapper, alertSinks));
        }
        return List.copyOf(result);
    }
}
