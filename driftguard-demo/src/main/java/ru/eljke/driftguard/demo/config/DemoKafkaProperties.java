package ru.eljke.driftguard.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * English demo documentation.
 * English demo documentation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo.kafka")
public class DemoKafkaProperties {
    /**
     * English demo documentation.
     * English demo documentation.
     */
    private boolean enabled = true;

    /**
     * English demo documentation.
     */
    private String bootstrapServers = "localhost:9092";

    /**
     * English demo documentation.
     */
    private String inputTopic = "driftguard.demo.metrics";

    /**
     * English demo documentation.
     */
    private String outputTopic = "driftguard.demo.drift-events";

    /**
     * English demo documentation.
     * English demo documentation.
     * English demo documentation.
     */
    private String applicationId = "driftguard-demo-streams";

    /**
     * English demo documentation.
     */
    private String consumerGroup = "driftguard-demo-ui";

    /**
     * English demo documentation.
     */
    private Duration playbackInterval = Duration.ofMillis(150);
}


