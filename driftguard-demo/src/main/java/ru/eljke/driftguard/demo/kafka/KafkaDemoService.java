package ru.eljke.driftguard.demo.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.core.error.DriftGuardValidationException;
import ru.eljke.driftguard.demo.config.DemoKafkaProperties;
import ru.eljke.driftguard.demo.error.DemoErrorReason;
import ru.eljke.driftguard.demo.scenario.DemoScenarioService;
import ru.eljke.driftguard.kafka.DriftGuardObjectMapper;
import ru.eljke.driftguard.kafka.DriftGuardSerdes;
import ru.eljke.driftguard.spring.DriftGuardKafkaStreamsManager;
import ru.eljke.driftguard.testkit.GradualDriftScenario;
import ru.eljke.driftguard.testkit.MetricScenario;
import ru.eljke.driftguard.testkit.PulseSpikeScenario;
import ru.eljke.driftguard.testkit.StepDriftScenario;
import ru.eljke.driftguard.testkit.ThroughputDropScenario;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Управляет реальным demo-контуром Kafka: создаёт topic-и, запускает topology
 * через Spring Boot starter DriftGuard, публикует тестовые метрики producer-ом
 * и читает найденные события drift-а consumer-ом для отображения в UI.
 */
@Service
public class KafkaDemoService {
    private final DemoKafkaProperties properties;
    private final DriftGuardKafkaStreamsManager streamsManager;
    private final ObjectMapper objectMapper;
    private final AtomicLong runSequence = new AtomicLong();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "driftguard-kafka-demo");
        thread.setDaemon(true);
        return thread;
    });

    private final List<DriftEvent> consumedEvents = new CopyOnWriteArrayList<>();
    private final List<MetricPoint> producedSamples = new CopyOnWriteArrayList<>();
    private final List<ProducerPlayback> producerPlaybacks = new CopyOnWriteArrayList<>();
    private final AtomicBoolean consuming = new AtomicBoolean();
    private volatile KafkaConsumer<String, DriftEvent> consumer;
    private volatile boolean running;
    private volatile String scenarioId = "latency-step";
    private volatile int totalPoints;
    private volatile String error;

    public KafkaDemoService(DemoKafkaProperties properties, DriftGuardKafkaStreamsManager streamsManager) {
        this.properties = properties;
        this.streamsManager = streamsManager;
        this.objectMapper = DriftGuardObjectMapper.create();
    }

    public synchronized KafkaDemoStatus start(String scenario) {
        if (!properties.isEnabled()) {
            throw new DriftGuardValidationException(DemoErrorReason.KAFKA_DEMO_DISABLED);
        }
        stop();
        scenarioId = scenario == null || scenario.isBlank() ? "latency-step" : scenario.trim();
        consumedEvents.clear();
        producedSamples.clear();
        producerPlaybacks.clear();
        error = null;

        long run = runSequence.incrementAndGet();
        List<ProducerPlayback> playbacks = createProducerPlaybacks(scenarioId, run);
        producerPlaybacks.addAll(playbacks);
        totalPoints = playbacks.stream().mapToInt(ProducerPlayback::totalPoints).sum();

        try {
            createTopics();
            streamsManager.start();

            consumer = new KafkaConsumer<>(consumerProperties(run), Serdes.String().deserializer(),
                    DriftGuardSerdes.driftEvent(objectMapper).deserializer());
            consumer.subscribe(List.of(properties.getOutputTopic()));
            consumer.poll(Duration.ofMillis(500));
            consuming.set(true);
            executor.execute(this::consumeEvents);

            playbacks.forEach(this::scheduleProducer);
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
        closeProducers();
        closeConsumer();
        streamsManager.stop();
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
                producerPlaybacks.stream().map(ProducerPlayback::status).toList(),
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

    private List<ProducerPlayback> createProducerPlaybacks(String scenario, long run) {
        if ("microservices-system".equals(scenario)) {
            return List.of(
                    playback("checkout-latency-producer", checkoutLatency(run)),
                    playback("payment-errors-producer", paymentErrors(run)),
                    playback("orders-queue-producer", ordersQueue(run)),
                    playback("checkout-throughput-producer", checkoutThroughput(run))
            );
        }
        return List.of(playback(scenario + "-producer", DemoScenarioService.createScenario(scenario, "kafka-run-" + run)));
    }

    private static MetricScenario checkoutLatency(long run) {
        return new StepDriftScenario(
                "checkout-latency-step",
                DemoScenarioService.config("checkout-service", "latency", "checkout-" + run, "POST /checkout", MetricKind.DURATION, 160),
                80,
                100.0,
                260.0,
                4.0
        );
    }

    private static MetricScenario paymentErrors(long run) {
        return new PulseSpikeScenario(
                "payment-error-rate-spike",
                DemoScenarioService.config("payment-service", "error-rate", "payment-" + run, "POST /payments", MetricKind.RATE, 140),
                60,
                28,
                0.01,
                0.18,
                0.002
        );
    }

    private static MetricScenario ordersQueue(long run) {
        return new GradualDriftScenario(
                "orders-queue-growth",
                DemoScenarioService.config("orders-worker", "queue-size", "orders-" + run, "orders.created", MetricKind.SIZE, 160),
                55,
                40.0,
                2.6,
                4.0
        );
    }

    private static MetricScenario checkoutThroughput(long run) {
        return new ThroughputDropScenario(
                "checkout-throughput-drop",
                DemoScenarioService.config("checkout-service", "throughput", "checkout-throughput-" + run, "POST /checkout", MetricKind.RATE, 150),
                70,
                1000.0,
                430.0,
                18.0
        );
    }

    private ProducerPlayback playback(String id, MetricScenario scenario) {
        List<MetricPoint> points = scenario.generate();
        MetricPoint first = points.getFirst();
        return new ProducerPlayback(
                id,
                first.key().service(),
                first.key().metric(),
                first.key().operation(),
                points,
                new KafkaProducer<>(producerProperties(), Serdes.String().serializer(),
                        DriftGuardSerdes.metricPoint(objectMapper).serializer())
        );
    }

    private void scheduleProducer(ProducerPlayback playback) {
        ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> {
            if (!playback.publishNext()) {
                playback.cancel();
                if (producerPlaybacks.stream().noneMatch(ProducerPlayback::running)) {
                    running = false;
                }
            }
        }, 0, Math.max(20, properties.getPlaybackInterval().toMillis()), TimeUnit.MILLISECONDS);
        playback.setTask(task);
    }

    private void consumeEvents() {
        try {
            while (consuming.get()) {
                consumer.poll(Duration.ofMillis(250)).forEach(record -> consumedEvents.add(record.value()));
            }
        } catch (WakeupException exception) {
            if (consuming.get()) {
                error = exception.getMessage();
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
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
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

    private void closeProducers() {
        producerPlaybacks.forEach(ProducerPlayback::close);
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

    private final class ProducerPlayback {
        private final String id;
        private final String service;
        private final String metric;
        private final String operation;
        private final List<MetricPoint> points;
        private final KafkaProducer<String, MetricPoint> producer;
        private final AtomicInteger index = new AtomicInteger();
        private volatile ScheduledFuture<?> task;

        private ProducerPlayback(
                String id,
                String service,
                String metric,
                String operation,
                List<MetricPoint> points,
                KafkaProducer<String, MetricPoint> producer
        ) {
            this.id = id;
            this.service = service;
            this.metric = metric;
            this.operation = operation;
            this.points = points;
            this.producer = producer;
        }

        private boolean publishNext() {
            int current = index.getAndIncrement();
            if (current >= points.size()) {
                return false;
            }
            MetricPoint point = points.get(current);
            producedSamples.add(point);
            producer.send(new ProducerRecord<>(properties.getInputTopic(), metricKey(point), point));
            producer.flush();
            return current + 1 < points.size();
        }

        private int totalPoints() {
            return points.size();
        }

        private boolean running() {
            ScheduledFuture<?> current = task;
            return current != null && !current.isDone() && !current.isCancelled();
        }

        private void setTask(ScheduledFuture<?> task) {
            this.task = task;
        }

        private void cancel() {
            ScheduledFuture<?> current = task;
            if (current != null) {
                current.cancel(false);
            }
        }

        private void close() {
            cancel();
            producer.close(Duration.ofSeconds(2));
        }

        private KafkaProducerStatus status() {
            return new KafkaProducerStatus(
                    id,
                    service,
                    metric,
                    operation,
                    Math.min(index.get(), points.size()),
                    points.size(),
                    running()
            );
        }
    }
}
