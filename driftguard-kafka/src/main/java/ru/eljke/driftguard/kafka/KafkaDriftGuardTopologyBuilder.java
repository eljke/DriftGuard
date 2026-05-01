package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Objects;

/**
 * Builder Kafka Streams topology для потоковой обработки метрик DriftGuard.
 */
public final class KafkaDriftGuardTopologyBuilder {
    private final ObjectMapper objectMapper;
    private final DriftDetectorEngine detectorEngine;

    public KafkaDriftGuardTopologyBuilder(ObjectMapper objectMapper, DriftDetectorEngine detectorEngine) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.detectorEngine = Objects.requireNonNull(detectorEngine, "detectorEngine must not be null");
    }

    public Topology build(KafkaDriftGuardTopologyConfig config) {
        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(config.inputTopics(), Consumed.with(Serdes.String(), DriftGuardSerdes.metricPoint(objectMapper)))
                .flatMapValues(detectorEngine::detect)
                .selectKey((key, event) -> event.id())
                .to(config.outputTopic(), Produced.with(Serdes.String(), DriftGuardSerdes.driftEvent(objectMapper)));
        return builder.build();
    }
}
