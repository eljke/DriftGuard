package ru.eljke.driftguard.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaDriftGuardTopologyBuilderTest {

    @Test
    void repartitionsMetricsByStableMetricIdentityBeforeDetection() {
        KafkaDriftGuardTopologyBuilder builder = new KafkaDriftGuardTopologyBuilder(
                DriftGuardObjectMapper.create(),
                point -> List.of()
        );

        String description = builder.build(new KafkaDriftGuardTopologyConfig(List.of("metrics"), "drift-events"))
                .describe()
                .toString();

        assertTrue(description.contains("driftguard-metric-key-repartition"), description);
    }

    @Test
    void canSkipFailedDetectionAndContinueProcessing() {
        AtomicInteger emittedEvents = new AtomicInteger();
        KafkaDriftGuardTopologyBuilder builder = new KafkaDriftGuardTopologyBuilder(
                DriftGuardObjectMapper.create(),
                point -> {
                    if (point.value() == 500.0) {
                        throw new IllegalStateException("detector failed");
                    }
                    return List.of(event(point, "event-" + emittedEvents.incrementAndGet()));
                },
                KafkaDetectionErrorHandler.skip()
        );
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "driftguard-topology-error-test");
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
            input.pipeInput("bad", MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z"), 500.0));
            input.pipeInput("good", MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:01Z"), 120.0));

            assertEquals("event-1", output.readValue().id());
            assertTrue(output.isEmpty());
        }
    }

    @Test
    void routesDetectedEventsToOutputTopic() {
        DriftDetectorEngine engine = new DriftDetectorEngine(
                DefaultAlgorithms.registry(),
                new InMemoryDetectorStateStore(),
                List.of(new DetectorDefinition(
                        "latency-page-hinkley",
                        new PageHinkleyConfig(3, 0.1, 5.0, 10.0, 0.05),
                        key -> key.metric().equals("latency")
                ))
        );

        KafkaDriftGuardTopologyBuilder builder = new KafkaDriftGuardTopologyBuilder(DriftGuardObjectMapper.create(), engine);
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "driftguard-topology-test");
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
                input.pipeInput("checkout:latency", MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i), 100.0));
            }
            for (int i = 6; i < 12; i++) {
                input.pipeInput("checkout:latency", MetricPoint.gauge(key, Instant.parse("2026-05-01T10:00:00Z").plusSeconds(i), 180.0));
            }

            assertFalse(output.isEmpty());
        }
    }

    private static DriftEvent event(MetricPoint point, String id) {
        return new DriftEvent(
                id,
                point.key(),
                point.timestamp(),
                point.timestamp(),
                point.timestamp(),
                DriftDirection.UP,
                DriftSeverity.WARNING,
                1.0,
                point.value(),
                100.0,
                "test-detector",
                "test-algorithm",
                "synthetic event",
                Map.of(),
                Map.of()
        );
    }
}
