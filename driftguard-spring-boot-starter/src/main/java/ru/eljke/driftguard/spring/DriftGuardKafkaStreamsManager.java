package ru.eljke.driftguard.spring;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.context.SmartLifecycle;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Duration;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Управляет жизненным циклом Kafka Streams topology DriftGuard внутри Spring
 * приложения.
 *
 * <p>Класс создаёт {@link KafkaStreams} лениво при {@link #start()}, чтобы
 * Spring context мог загружаться даже в тестах, где auto-start отключён. При
 * остановке context-а streams закрывается через штатный {@code close()}.</p>
 */
public class DriftGuardKafkaStreamsManager implements SmartLifecycle {
    private final DriftGuardProperties.KafkaProperties properties;
    private final Supplier<KafkaStreams> streamsFactory;
    private volatile KafkaStreams streams;
    private volatile boolean running;

    public DriftGuardKafkaStreamsManager(
            DriftGuardProperties.KafkaProperties properties,
            Supplier<KafkaStreams> streamsFactory
    ) {
        this.properties = properties;
        this.streamsFactory = streamsFactory;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        streams = streamsFactory.get();
        streams.start();
        running = true;
    }

    @Override
    public synchronized void stop() {
        KafkaStreams current = streams;
        streams = null;
        running = false;
        if (current != null) {
            current.close(Duration.ofSeconds(10));
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.isAutoStartup();
    }

    Properties streamsProperties() {
        return streamsProperties(properties);
    }

    public static Properties streamsProperties(DriftGuardProperties.KafkaProperties properties) {
        Properties props = new Properties();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, properties.getApplicationId());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        String stateDir = properties.getStateDir();
        if (stateDir != null && !stateDir.isBlank()) {
            props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir.trim());
        }
        return props;
    }

    static void validate(DriftGuardProperties.KafkaProperties properties) {
        if (properties.getInputTopics().isEmpty()) {
            throw DriftGuardErrors.validation(SpringDriftGuardErrorReason.EMPTY_KAFKA_INPUT_TOPICS);
        }
        if (properties.getOutputTopic() == null || properties.getOutputTopic().isBlank()) {
            throw DriftGuardErrors.validation(SpringDriftGuardErrorReason.BLANK_KAFKA_OUTPUT_TOPIC);
        }
        if (properties.getBootstrapServers() == null || properties.getBootstrapServers().isBlank()) {
            throw DriftGuardErrors.validation(SpringDriftGuardErrorReason.BLANK_KAFKA_BOOTSTRAP_SERVERS);
        }
        if (properties.getApplicationId() == null || properties.getApplicationId().isBlank()) {
            throw DriftGuardErrors.validation(SpringDriftGuardErrorReason.BLANK_KAFKA_APPLICATION_ID);
        }
    }
}
