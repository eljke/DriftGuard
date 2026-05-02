package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builder Kafka Streams topology для потоковой обработки метрик DriftGuard.
 */
public final class KafkaDriftGuardTopologyBuilder {
    private final ObjectMapper objectMapper;
    private final Function<MetricPoint, List<DriftEvent>> detector;

    public KafkaDriftGuardTopologyBuilder(ObjectMapper objectMapper, DriftDetectorEngine detectorEngine) {
        this(objectMapper, detectorEngine::detect);
    }

    public KafkaDriftGuardTopologyBuilder(ObjectMapper objectMapper, Function<MetricPoint, List<DriftEvent>> detector) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.detector = Objects.requireNonNull(detector, "detector must not be null");
    }

    public Topology build(KafkaDriftGuardTopologyConfig config) {
        StreamsBuilder builder = new StreamsBuilder();
        var metricPointSerde = DriftGuardSerdes.metricPoint(objectMapper);
        var driftEventSerde = DriftGuardSerdes.driftEvent(objectMapper);

        builder.stream(config.inputTopics(), Consumed.with(Serdes.String(), metricPointSerde))
                .filter((ignored, point) -> point != null)
                .selectKey((ignored, point) -> KafkaMetricKeys.stateKey(point.key()))
                .repartition(Repartitioned.with(Serdes.String(), metricPointSerde)
                        .withName("driftguard-metric-key"))
                .flatMapValues(detector::apply)
                .selectKey((ignored, event) -> event.id())
                .to(config.outputTopic(), Produced.with(Serdes.String(), driftEventSerde));
        return builder.build();
    }
}
