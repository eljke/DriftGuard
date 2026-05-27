package ru.eljke.driftguard.kafka.topology;
import ru.eljke.driftguard.kafka.error.*;
import ru.eljke.driftguard.kafka.serde.*;
import ru.eljke.driftguard.kafka.state.*;
import ru.eljke.driftguard.kafka.telemetry.*;
import ru.eljke.driftguard.kafka.topology.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.algorithms.state.BuiltInDetectorStateCodecs;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaStatefulDriftGuardTopologyBuilderTest {

    @Test
    void storesDetectorRuntimeStateInKafkaStateStore() {
        KafkaStatefulDriftGuardTopologyBuilder builder = new KafkaStatefulDriftGuardTopologyBuilder(
                DriftGuardObjectMapper.create(),
                DefaultAlgorithms.registry(),
                BuiltInDetectorStateCodecs.registry(),
                List.of(new DetectorDefinition(
                        "latency-page-hinkley",
                        new PageHinkleyConfig(3, 0.1, 5.0, 10.0, 0.05),
                        key -> key.metric().equals("latency")
                ))
        );
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "driftguard-stateful-topology-test");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver driver = new TopologyTestDriver(
                builder.build(new KafkaDriftGuardTopologyConfig(List.of("metrics"), "drift-events")),
                properties
        )) {
            TestInputTopic<String, MetricPoint> input = driver.createInputTopic(
                    "metrics",
                    Serdes.String().serializer(),
                    DriftGuardSerdes.metricPoint(DriftGuardObjectMapper.create()).serializer()
            );
            TestOutputTopic<String, DriftEvent> output = driver.createOutputTopic(
                    "drift-events",
                    Serdes.String().deserializer(),
                    DriftGuardSerdes.driftEvent(DriftGuardObjectMapper.create()).deserializer()
            );

            MetricKey key = MetricKey.of("checkout", "latency");
            for (int i = 0; i < 6; i++) {
                input.pipeInput("any-key-" + i, MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i), 100.0));
            }
            for (int i = 6; i < 12; i++) {
                input.pipeInput("different-key-" + i, MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i), 180.0));
            }

            assertFalse(output.isEmpty());
            KeyValueStore<String, DetectorRuntimeStateSnapshot> stateStore = driver.getKeyValueStore(
                    KafkaDriftGuardStateStores.DEFAULT_RUNTIME_STATE_STORE
            );
            String stateKey = KafkaDetectorInstanceKeys.stateKey(new DetectorInstanceKey(key, "latency-page-hinkley"));
            DetectorRuntimeStateSnapshot snapshot = stateStore.get(stateKey);
            assertNotNull(snapshot);
            assertTrue(snapshot.version() > 0);
        }
    }
}
