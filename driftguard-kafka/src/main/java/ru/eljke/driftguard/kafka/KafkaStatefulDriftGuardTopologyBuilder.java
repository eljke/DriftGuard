package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorSupplier;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.DriftGuardErrors;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateSnapshot;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateSnapshotCodec;
import ru.eljke.driftguard.core.state.DetectorStateCodecRegistry;

import java.time.Instant;
import java.util.List;

/**
 * Kafka Streams topology builder with a state store for detector runtime state.
 *
 * <p>This builder creates {@link DriftDetectorEngine} inside a stream task and binds
 * it to the local Kafka state store. This lets detector state and emission state survive
 * restart/rebalance through the Kafka Streams changelog.</p>
 */
public final class KafkaStatefulDriftGuardTopologyBuilder {
    private final ObjectMapper objectMapper;
    private final DetectorRegistry detectorRegistry;
    private final DetectorStateCodecRegistry stateCodecRegistry;
    private final List<DetectorDefinition> definitions;
    private final KafkaDetectionErrorHandler detectionErrorHandler;
    private final String runtimeStateStoreName;
    private final List<KafkaDetectionTelemetryListener> telemetryListeners;

    public KafkaStatefulDriftGuardTopologyBuilder(
            ObjectMapper objectMapper,
            DetectorRegistry detectorRegistry,
            DetectorStateCodecRegistry stateCodecRegistry,
            List<DetectorDefinition> definitions
    ) {
        this(
                objectMapper,
                detectorRegistry,
                stateCodecRegistry,
                definitions,
                KafkaDetectionErrorHandler.failFast(),
                KafkaDriftGuardStateStores.DEFAULT_RUNTIME_STATE_STORE,
                List.of()
        );
    }

    public KafkaStatefulDriftGuardTopologyBuilder(
            ObjectMapper objectMapper,
            DetectorRegistry detectorRegistry,
            DetectorStateCodecRegistry stateCodecRegistry,
            List<DetectorDefinition> definitions,
            KafkaDetectionErrorHandler detectionErrorHandler,
            String runtimeStateStoreName
    ) {
        this(
                objectMapper,
                detectorRegistry,
                stateCodecRegistry,
                definitions,
                detectionErrorHandler,
                runtimeStateStoreName,
                List.of()
        );
    }

    public KafkaStatefulDriftGuardTopologyBuilder(
            ObjectMapper objectMapper,
            DetectorRegistry detectorRegistry,
            DetectorStateCodecRegistry stateCodecRegistry,
            List<DetectorDefinition> definitions,
            KafkaDetectionErrorHandler detectionErrorHandler,
            String runtimeStateStoreName,
            List<KafkaDetectionTelemetryListener> telemetryListeners
    ) {
        this.objectMapper = DriftGuardErrors.requireNonNull(objectMapper, "objectMapper");
        this.detectorRegistry = DriftGuardErrors.requireNonNull(detectorRegistry, "detectorRegistry");
        this.stateCodecRegistry = DriftGuardErrors.requireNonNull(stateCodecRegistry, "stateCodecRegistry");
        this.definitions = List.copyOf(definitions == null ? List.of() : definitions);
        this.detectionErrorHandler = DriftGuardErrors.requireNonNull(detectionErrorHandler, "detectionErrorHandler");
        this.runtimeStateStoreName = DriftGuardErrors.requireNonBlank(runtimeStateStoreName, "runtimeStateStoreName");
        this.telemetryListeners = List.copyOf(telemetryListeners == null ? List.of() : telemetryListeners);
    }

    public Topology build(KafkaDriftGuardTopologyConfig config) {
        StreamsBuilder builder = new StreamsBuilder();
        var metricPointSerde = DriftGuardSerdes.metricPoint(objectMapper);
        var driftEventSerde = DriftGuardSerdes.driftEvent(objectMapper);
        var detectionErrorSerde = DriftGuardSerdes.detectionError(objectMapper);

        builder.addStateStore(KafkaDriftGuardStateStores.runtimeStateStore(runtimeStateStoreName, objectMapper));
        var detectionResults = builder.stream(config.inputTopics(), Consumed.with(Serdes.String(), metricPointSerde))
                .filter((ignored, point) -> point != null)
                .selectKey((ignored, point) -> KafkaMetricKeys.stateKey(point.key()))
                .repartition(Repartitioned.with(Serdes.String(), metricPointSerde)
                        .withName("driftguard-metric-key"))
                .processValues(new DetectingProcessorSupplier(), runtimeStateStoreName);

        detectionResults
                .flatMapValues(DetectionResult::events)
                .selectKey((ignored, event) -> event.id())
                .to(config.outputTopic(), Produced.with(Serdes.String(), driftEventSerde));

        if (config.detectionErrorTopic() != null) {
            detectionResults
                    .filter((ignored, result) -> result.error() != null)
                    .mapValues(DetectionResult::error)
                    .peek((ignored, error) -> notifyErrorRouted(error))
                    .selectKey((ignored, error) -> KafkaMetricKeys.stateKey(error.point().key()))
                    .to(config.detectionErrorTopic(), Produced.with(Serdes.String(), detectionErrorSerde));
        }

        return builder.build();
    }

    private final class DetectingProcessorSupplier implements FixedKeyProcessorSupplier<String, MetricPoint, DetectionResult> {
        @Override
        public FixedKeyProcessor<String, MetricPoint, DetectionResult> get() {
            return new DetectingProcessor();
        }
    }

    private final class DetectingProcessor implements FixedKeyProcessor<String, MetricPoint, DetectionResult> {
        private FixedKeyProcessorContext<String, DetectionResult> context;
        private DriftDetectorEngine detectorEngine;

        @Override
        public void init(FixedKeyProcessorContext<String, DetectionResult> context) {
            this.context = context;
            KeyValueStore<String, DetectorRuntimeStateSnapshot> store = context.getStateStore(runtimeStateStoreName);
            var runtimeStateStore = new KafkaDetectorRuntimeStateStore(
                    store,
                    new DetectorRuntimeStateSnapshotCodec(stateCodecRegistry)
            );
            detectorEngine = new DriftDetectorEngine(detectorRegistry, runtimeStateStore, definitions);
        }

        @Override
        public void process(FixedKeyRecord<String, MetricPoint> record) {
            DetectionResult result;
            long startedAt = System.nanoTime();
            try {
                List<DriftEvent> events = emptyIfNull(detectorEngine.detect(record.value()));
                notifyCompleted(record.value(), events, System.nanoTime() - startedAt);
                result = DetectionResult.events(events);
            } catch (RuntimeException exception) {
                notifyFailed(record.value(), exception, System.nanoTime() - startedAt);
                result = DetectionResult.failure(
                        emptyIfNull(detectionErrorHandler.handle(record.value(), exception)),
                        KafkaDetectionError.from(record.value(), exception, Instant.ofEpochMilli(record.timestamp()))
                );
            }
            context.forward(record.withValue(result));
        }

        @Override
        public void close() {
            // Kafka Streams manages the state store lifecycle.
        }
    }

    private record DetectionResult(List<DriftEvent> events, KafkaDetectionError error) {
        private DetectionResult {
            events = emptyIfNull(events);
        }

        private static DetectionResult events(List<DriftEvent> events) {
            return new DetectionResult(events, null);
        }

        private static DetectionResult failure(List<DriftEvent> fallbackEvents, KafkaDetectionError error) {
            return new DetectionResult(fallbackEvents, error);
        }
    }

    private void notifyCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
        for (KafkaDetectionTelemetryListener listener : telemetryListeners) {
            listener.onDetectionCompleted(point, events, durationNanos);
        }
    }

    private void notifyFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
        for (KafkaDetectionTelemetryListener listener : telemetryListeners) {
            listener.onDetectionFailed(point, exception, durationNanos);
        }
    }

    private void notifyErrorRouted(KafkaDetectionError error) {
        for (KafkaDetectionTelemetryListener listener : telemetryListeners) {
            listener.onDetectionErrorRouted(error);
        }
    }

    private static List<DriftEvent> emptyIfNull(List<DriftEvent> events) {
        return events == null ? List.of() : events;
    }
}


