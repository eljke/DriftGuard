package ru.eljke.driftguard.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Настройки интеграционного demo-контура, в котором метрики проходят через
 * Kafka producer, Kafka Streams topology DriftGuard и Kafka consumer событий.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo.kafka")
public class DemoKafkaProperties {
    /**
     * Включает REST endpoints Kafka demo. При выключении UI может продолжать
     * показывать synthetic demo без обращения к Kafka.
     */
    private boolean enabled = true;

    /**
     * Kafka bootstrap servers для producer, consumer, admin client и streams.
     */
    private String bootstrapServers = "localhost:9092";

    /**
     * Topic, куда demo producer публикует {@code MetricPoint}.
     */
    private String inputTopic = "driftguard.demo.metrics";

    /**
     * Topic, куда DriftGuard Kafka topology публикует {@code DriftEvent}.
     */
    private String outputTopic = "driftguard.demo.drift-events";

    /**
     * Базовый Kafka Streams application id. Для каждого запуска demo к нему
     * добавляется номер run-а, чтобы состояние предыдущих прогонов не мешало
     * текущей демонстрации.
     */
    private String applicationId = "driftguard-demo-streams";

    /**
     * Базовый consumer group id для чтения событий в demo UI.
     */
    private String consumerGroup = "driftguard-demo-ui";

    /**
     * Интервал публикации тестовых метрик producer-ом.
     */
    private Duration playbackInterval = Duration.ofMillis(150);
}
