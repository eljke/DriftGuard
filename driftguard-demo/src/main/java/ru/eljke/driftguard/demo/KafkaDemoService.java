package ru.eljke.driftguard.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
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
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.kafka.DriftGuardSerdes;
import ru.eljke.driftguard.kafka.DriftGuardObjectMapper;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyBuilder;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyConfig;
import ru.eljke.driftguard.testkit.MetricScenario;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Управляет реальным demo-контуром Kafka: создаёт topic-и, запускает Kafka
 * Streams topology DriftGuard, публикует тестовые метрики producer-ом и читает
 * найденные события drift-а consumer-ом для отображения в UI.
 */
@Service
public class KafkaDemoService {
    private final DemoKafkaProperties properties;
    private final ObjectMapper objectMapper;
    private final DriftDetectorEngine engine;
    private final AtomicLong runSequence = new AtomicLong();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "driftguard-kafka-demo");
        thread.setDaemon(true);
        return thread;
    });

    private final List<DriftEvent> consumedEvents = new CopyOnWriteArrayList<>();
    private final List<MetricPoint> producedSamples = new CopyOnWriteArrayList<>();
    private final AtomicBoolean consuming = new AtomicBoolean();
    private volatile KafkaStreams streams;
    private volatile KafkaProducer<String, MetricPoint> producer;
    private volatile KafkaConsumer<String, DriftEvent> consumer;
    private volatile ScheduledFuture<?> producerTask;
    private volatile boolean running;
    private volatile String scenarioId = "latency-step";
    private volatile int totalPoints;
    private volatile String error;

    public KafkaDemoService(DemoKafkaProperties properties, DriftDetectorEngine engine) {
        this.properties = properties;
        this.objectMapper = DriftGuardObjectMapper.create();
        this.engine = engine;
    }

    public synchronized KafkaDemoStatus start(String scenario) {
        if (!properties.isEnabled()) {
            throw new DriftGuardValidationException(DemoErrorReason.KAFKA_DEMO_DISABLED);
        }
        stop();
        scenarioId = scenario == null || scenario.isBlank() ? "latency-step" : scenario.trim();
        consumedEvents.clear();
        producedSamples.clear();
        error = null;

        long run = runSequence.incrementAndGet();
        MetricScenario metricScenario = DemoScenarioService.createScenario(scenarioId, "kafka-run-" + run);
        List<MetricPoint> points = metricScenario.generate();
        totalPoints = points.size();

        try {
            createTopics();
            streams = new KafkaStreams(
                    new KafkaDriftGuardTopologyBuilder(objectMapper, engine)
                            .build(new KafkaDriftGuardTopologyConfig(List.of(properties.getInputTopic()), properties.getOutputTopic())),
                    streamsProperties(run)
            );
            streams.start();

            consumer = new KafkaConsumer<>(consumerProperties(run), Serdes.String().deserializer(),
                    DriftGuardSerdes.driftEvent(objectMapper).deserializer());
            consumer.subscribe(List.of(properties.getOutputTopic()));
            consuming.set(true);
            executor.execute(this::consumeEvents);

            producer = new KafkaProducer<>(producerProperties(), Serdes.String().serializer(),
                    DriftGuardSerdes.metricPoint(objectMapper).serializer());
            producerTask = executor.scheduleAtFixedRate(new Runnable() {
                private int index;

                @Override
                public void run() {
                    if (index >= points.size()) {
                        running = false;
                        cancelProducerTask();
                        return;
                    }
                    MetricPoint point = points.get(index++);
                    producedSamples.add(point);
                    producer.send(new ProducerRecord<>(properties.getInputTopic(), metricKey(point), point));
                    producer.flush();
                }
            }, 0, Math.max(20, properties.getPlaybackInterval().toMillis()), TimeUnit.MILLISECONDS);
            running = true;
            return status();
        } catch (RuntimeException exception) {
            error = exception.getMessage();
            stop();
            throw new DriftGuardValidationException(DemoErrorReason.KAFKA_DEMO_FAILED, exception.getMessage());
        }
    }

    public synchronized KafkaDemoStatus stop() {
        running = false;
        cancelProducerTask();
        closeProducer();
        closeConsumer();
        closeStreams();
        return status();
    }

    public KafkaDemoStatus status() {
        return new KafkaDemoStatus(
                properties.isEnabled(),
                running,
                scenarioId,
                properties.getInputTopic(),
                properties.getOutputTopic(),
                properties.getBootstrapServers(),
                producedSamples.size(),
                totalPoints,
                List.copyOf(consumedEvents),
                List.copyOf(producedSamples),
                error
        );
    }

    @PreDestroy
    public void shutdown() {
        stop();
        executor.shutdownNow();
    }

    private void createTopics() {
        try (AdminClient admin = AdminClient.create(adminProperties())) {
            Set<String> existing = admin.listTopics().names().get();
            List<NewTopic> missing = new ArrayList<>();
            addMissingTopic(existing, missing, properties.getInputTopic());
            addMissingTopic(existing, missing, properties.getOutputTopic());
            if (!missing.isEmpty()) {
                admin.createTopics(missing).all().get();
            }
        } catch (Exception exception) {
            throw new DriftGuardValidationException(DemoErrorReason.KAFKA_DEMO_FAILED, exception.getMessage());
        }
    }

    private void consumeEvents() {
        try {
            while (consuming.get()) {
                consumer.poll(Duration.ofMillis(250)).forEach(record -> consumedEvents.add(record.value()));
            }
        } catch (RuntimeException exception) {
            error = exception.getMessage();
        }
    }

    private Properties adminProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        return props;
    }

    private Properties producerProperties() {
        Properties props = adminProperties();
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    private Properties consumerProperties(long run) {
        Properties props = adminProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumerGroup() + "-" + run);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private Properties streamsProperties(long run) {
        Properties props = adminProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, properties.getApplicationId() + "-" + run);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        return props;
    }

    private static void addMissingTopic(Collection<String> existing, List<NewTopic> missing, String topic) {
        if (!existing.contains(topic)) {
            missing.add(new NewTopic(topic, 1, (short) 1));
        }
    }

    private static String metricKey(MetricPoint point) {
        return String.join("|",
                point.key().service(),
                point.key().metric(),
                String.valueOf(point.key().instance()),
                String.valueOf(point.key().operation())
        );
    }

    private void cancelProducerTask() {
        ScheduledFuture<?> task = producerTask;
        if (task != null) {
            task.cancel(false);
            producerTask = null;
        }
    }

    private void closeProducer() {
        KafkaProducer<String, MetricPoint> current = producer;
        producer = null;
        if (current != null) {
            current.close(Duration.ofSeconds(2));
        }
    }

    private void closeConsumer() {
        consuming.set(false);
        KafkaConsumer<String, DriftEvent> current = consumer;
        consumer = null;
        if (current != null) {
            current.wakeup();
            current.close(Duration.ofSeconds(2));
        }
    }

    private void closeStreams() {
        KafkaStreams current = streams;
        streams = null;
        if (current != null) {
            current.close(Duration.ofSeconds(3));
        }
    }
}
