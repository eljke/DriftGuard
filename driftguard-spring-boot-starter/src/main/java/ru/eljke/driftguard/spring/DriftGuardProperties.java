package ru.eljke.driftguard.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.eljke.driftguard.core.domain.DriftDirection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DriftGuard settings read from {@code application.yml}.
 *
 * <p>The class is the public configuration contract of the starter. All fields
 * use Spring Boot relaxed binding: for example {@code baselineWindowSize}
 * is configured as {@code baseline-window-size}. If {@link #detectors} is empty,
 * the starter creates a small default detector set for demo/quick-start mode.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "driftguard")
public class DriftGuardProperties {
    /**
     * Enables DriftGuard auto-configuration.
     *
     * <p>When {@code false} starter does not create registry, state store, detector
     * definitions and {@code DriftDetectorEngine}.</p>
     */
    private boolean enabled = true;

    /**
     * Set of detector definitions applied to incoming {@code MetricPoint}.
     *
     * <p>Each item configures an algorithm, metric-stream filter, windows, thresholds and
     * event publication policy.</p>
     */
    private List<DetectorProperties> detectors = new ArrayList<>();

    /**
     * Global flag for detector definitions declared in {@code application.yml}.
     */
    private boolean detectorsEnabled = true;

    /**
     * DriftGuard runtime metrics settings.
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * Kafka Streams adapter settings.
     *
     * <p>The Kafka part is disabled by default because not every Spring
     * application needs a transport adapter. When {@code kafka.enabled} is true,
     * the starter creates and starts a Kafka Streams topology that reads
     * {@code MetricPoint} objects from input topics and writes {@code DriftEvent}
     * objects to the output topic.</p>
     */
    private KafkaProperties kafka = new KafkaProperties();

    public void setDetectors(List<DetectorProperties> detectors) {
        this.detectors = detectors == null ? new ArrayList<>() : detectors;
    }

    public void setKafka(KafkaProperties kafka) {
        this.kafka = kafka == null ? new KafkaProperties() : kafka;
    }

    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics == null ? new MetricsProperties() : metrics;
    }

    @Getter
    @Setter
    public static class DetectorProperties {
        /**
         * Allows temporarily disabling a detector without removing its configuration.
         */
        private boolean enabled = true;

        /**
         * Detector name stored in {@code DriftEvent.detector} and in the state key.
         */
        private String name;

        /**
         * Algorithm name: {@code page-hinkley}, {@code adwin}, {@code psi},
         * {@code ks} or {@code chi-square}.
         */
        private String algorithm;

        /**
         * Allowed values of {@code MetricKey.service}. An empty list
         * means the detector applies to any service.
         */
        private List<String> services = new ArrayList<>();

        /**
         * Allowed values of {@code MetricKey.metric}. An empty list
         * means the detector applies to any metric.
         */
        private List<String> metrics = new ArrayList<>();

        /**
         * Allowed values of {@code MetricKey.operation}. An empty list
         * means the detector applies to any operation.
         */
        private List<String> operations = new ArrayList<>();

        /**
         * Allowed values of {@code MetricKey.instance}. An empty list
         * means the detector applies to any instance id.
         */
        private List<String> instances = new ArrayList<>();

        /**
         * Baseline window size for PSI, KS and chi-square.
         */
        private int baselineWindowSize = 40;

        /**
         * Current window size for PSI, KS and chi-square.
         */
        private int currentWindowSize = 40;

        /**
         * Adaptive window size for ADWIN.
         */
        private int windowSize = 40;

        /**
         * Minimum size of each window side when searching for an ADWIN cut.
         */
        private int minSubWindowSize = 10;

        /**
         * Number of initial samples used by Page-Hinkley to warm up the baseline.
         */
        private int warmupSamples = 20;

        /**
         * Number of buckets for PSI and chi-square.
         */
        private int buckets = 5;

        /**
         * Warning threshold for PSI or Page-Hinkley.
         */
        private double warningThreshold = 0.1;

        /**
         * Critical threshold for PSI or Page-Hinkley.
         */
        private double criticalThreshold = 0.25;

        /**
         * P-value boundary for warning events in KS and chi-square.
         */
        private double warningPValue = 0.05;

        /**
         * P-value boundary for critical events in KS and chi-square.
         */
        private double criticalPValue = 0.01;

        /**
         * Sensitivity/confidence parameter for ADWIN and Page-Hinkley.
         */
        private double delta = 0.1;

        /**
         * Mean adaptation speed in Page-Hinkley.
         */
        private double alpha = 0.05;

        /**
         * Mean-shift direction searched by Page-Hinkley.
         *
         * <p>{@code UP} fits latency, error-rate and queue-size.
         * {@code DOWN} fits throughput and other metrics where a drop is dangerous.</p>
         */
        private DriftDirection direction = DriftDirection.UP;

        /**
         * PSI bucket smoothing that protects against division by zero.
         */
        private double epsilon = 1e-4;

        /**
         * Score multiplier after which ADWIN creates a critical event.
         */
        private double criticalMultiplier = 2.0;

        /**
         * Minimum expected bucket frequency for chi-square.
         */
        private double minExpectedCount = 1.0;

        /**
         * Event publication policy over raw algorithm signals.
         */
        private EmissionPolicyProperties emissionPolicy = new EmissionPolicyProperties();

        public void setServices(List<String> services) {
            this.services = services == null ? new ArrayList<>() : services;
        }

        public void setMetrics(List<String> metrics) {
            this.metrics = metrics == null ? new ArrayList<>() : metrics;
        }

        public void setOperations(List<String> operations) {
            this.operations = operations == null ? new ArrayList<>() : operations;
        }

        public void setInstances(List<String> instances) {
            this.instances = instances == null ? new ArrayList<>() : instances;
        }

        public void setEmissionPolicy(EmissionPolicyProperties emissionPolicy) {
            this.emissionPolicy = emissionPolicy == null ? new EmissionPolicyProperties() : emissionPolicy;
        }
    }

    @Getter
    @Setter
    public static class EmissionPolicyProperties {
        /**
         * How many consecutive algorithm signals are required before
         * publishing {@code DriftEvent}.
         */
        private int minConsecutiveSignals = 1;

        /**
         * Minimum pause between published events of one detector
         * for one metric stream.
         */
        private Duration cooldown = Duration.ZERO;

        /**
         * How many consecutive normal points are required for the current drift
         * episode to be considered finished so the detector can publish again.
         */
        private int recoveryConsecutiveNormal = 1;
    }

    @Getter
    @Setter
    public static class MetricsProperties {
        /**
         * Enables the Micrometer listener for runtime detection metrics.
         *
         * <p>The listener is created only when a {@code MeterRegistry} is present.
         * In a Spring Boot application it is usually provided by Actuator.</p>
         */
        private boolean enabled = true;

        /**
         * Enables Kafka-specific Micrometer topology metrics.
         *
         * <p>These metrics complement the generic detection listener with topology
         * errors, routed diagnostic messages and processing duration inside Kafka
         * Streams tasks.</p>
         */
        private boolean kafkaEnabled = true;
    }

    @Getter
    @Setter
    public static class KafkaProperties {
        /**
         * Enables the DriftGuard Kafka Streams topology.
         */
        private boolean enabled = false;

        /**
         * Kafka bootstrap servers, for example {@code localhost:9092}.
         */
        private String bootstrapServers = "localhost:9092";

        /**
         * Kafka Streams application id.
         */
        private String applicationId = "driftguard";

        /**
         * Topics containing JSON {@code MetricPoint} messages.
         */
        private List<String> inputTopics = new ArrayList<>();

        /**
         * Topic where the topology writes JSON {@code DriftEvent} messages.
         */
        private String outputTopic = "driftguard.drift-events";

        /**
         * Local Kafka Streams state-store directory. When empty, Kafka Streams uses
         * its default value.
         */
        private String stateDir;

        /**
         * Whether the topology should start automatically with the Spring context.
         */
        private boolean autoStartup = true;

        public void setInputTopics(List<String> inputTopics) {
            this.inputTopics = inputTopics == null ? new ArrayList<>() : inputTopics;
        }
    }
}


