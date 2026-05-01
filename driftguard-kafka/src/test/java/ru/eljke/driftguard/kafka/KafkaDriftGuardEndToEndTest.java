package ru.eljke.driftguard.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.algorithms.pagehinkley.PageHinkleyConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Проверяет полный Kafka-контур: producer пишет MetricPoint, Kafka Streams
 * topology DriftGuard обнаруживает drift, consumer читает DriftEvent из output
 * topic-а.
 */
@Testcontainers(disabledWithoutDocker = true)
class KafkaDriftGuardEndToEndTest {
    @Container
    private static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1")
    );

    @TempDir
    private Path stateDir;

    @Test
    void detectsDriftThroughRealKafkaBroker() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String inputTopic = "driftguard.metrics." + suffix;
        String outputTopic = "driftguard.events." + suffix;

        createTopics(inputTopic, outputTopic);

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
        Properties streamsProperties = kafkaProperties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "driftguard-e2e-" + suffix);
        streamsProperties.put(StreamsConfig.STATE_DIR_CONFIG, stateDir.toString());

        try (KafkaStreams streams = new KafkaStreams(
                builder.build(new KafkaDriftGuardTopologyConfig(List.of(inputTopic), outputTopic)),
                streamsProperties
        );
             KafkaProducer<String, MetricPoint> producer = producer();
             KafkaConsumer<String, DriftEvent> consumer = consumer("driftguard-e2e-consumer-" + suffix)) {
            consumer.subscribe(List.of(outputTopic));
            streams.start();
            waitForStreamsRunning(streams);

            publishLatencyDegradation(inputTopic, producer);

            List<DriftEvent> events = pollEvents(consumer, outputTopic, Duration.ofSeconds(30));
            assertFalse(events.isEmpty(), "Kafka topology must publish at least one DriftEvent");
        }
    }

    private static void createTopics(String inputTopic, String outputTopic) throws Exception {
        try (AdminClient admin = AdminClient.create(kafkaProperties())) {
            admin.createTopics(List.of(
                    new NewTopic(inputTopic, 1, (short) 1),
                    new NewTopic(outputTopic, 1, (short) 1)
            )).all().get();
        }
    }

    private static KafkaProducer<String, MetricPoint> producer() {
        Properties props = kafkaProperties();
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(
                props,
                Serdes.String().serializer(),
                DriftGuardSerdes.metricPoint(DriftGuardObjectMapper.create()).serializer()
        );
    }

    private static KafkaConsumer<String, DriftEvent> consumer(String groupId) {
        Properties props = kafkaProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(
                props,
                Serdes.String().deserializer(),
                DriftGuardSerdes.driftEvent(DriftGuardObjectMapper.create()).deserializer()
        );
    }

    private static Properties kafkaProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        return props;
    }

    private static void publishLatencyDegradation(String inputTopic, KafkaProducer<String, MetricPoint> producer) {
        MetricKey key = MetricKey.of("checkout-service", "latency");
        Instant start = Instant.parse("2026-05-01T10:00:00Z");
        for (int i = 0; i < 8; i++) {
            send(producer, inputTopic, key, start.plusSeconds(i), 100.0);
        }
        for (int i = 8; i < 24; i++) {
            send(producer, inputTopic, key, start.plusSeconds(i), 220.0);
        }
        producer.flush();
    }

    private static void send(
            KafkaProducer<String, MetricPoint> producer,
            String topic,
            MetricKey key,
            Instant timestamp,
            double value
    ) {
        producer.send(new ProducerRecord<>(topic, "checkout-service|latency", MetricPoint.gauge(key, timestamp, value)));
    }

    private static void waitForStreamsRunning(KafkaStreams streams) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            if (streams.state().isRunningOrRebalancing()) {
                return;
            }
            Thread.sleep(100);
        }
    }

    private static List<DriftEvent> pollEvents(KafkaConsumer<String, DriftEvent> consumer, String outputTopic, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            List<DriftEvent> events = new ArrayList<>();
            consumer.poll(Duration.ofMillis(250))
                    .records(outputTopic)
                    .forEach(record -> events.add(record.value()));
            if (!events.isEmpty()) {
                return events;
            }
        }
        return List.of();
    }
}
