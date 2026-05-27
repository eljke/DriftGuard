package ru.eljke.driftguard.spring.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import ru.eljke.driftguard.core.adapter.MetricPointPublisher;
import ru.eljke.driftguard.core.alert.DefaultDriftAlertMapper;
import ru.eljke.driftguard.core.alert.DriftAlertListener;
import ru.eljke.driftguard.core.alert.DriftAlertMapper;
import ru.eljke.driftguard.core.alert.DriftAlertSink;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.DetectorDefinitionProvider;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.detector.DriftDetectionListener;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.DriftEventSinkListener;
import ru.eljke.driftguard.core.detector.SimpleDetectorRegistry;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.sink.DriftEventSink;
import ru.eljke.driftguard.core.state.DetectorRuntimeStateStore;
import ru.eljke.driftguard.core.state.DetectorStateStore;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;
import ru.eljke.driftguard.core.state.InMemoryEmissionStateStore;
import ru.eljke.driftguard.core.state.SplitDetectorRuntimeStateStore;
import ru.eljke.driftguard.kafka.serde.DriftGuardObjectMapper;
import ru.eljke.driftguard.kafka.telemetry.KafkaDetectionTelemetryListener;
import ru.eljke.driftguard.kafka.topology.KafkaDriftGuardTopologyBuilder;
import ru.eljke.driftguard.kafka.topology.KafkaDriftGuardTopologyConfig;
import ru.eljke.driftguard.spring.alert.Slf4jDriftAlertSink;
import ru.eljke.driftguard.spring.alert.WebhookDriftAlertSink;
import ru.eljke.driftguard.spring.input.DriftGuardMetricPointPublisher;
import ru.eljke.driftguard.spring.input.MicrometerMetricInputPoller;
import ru.eljke.driftguard.spring.kafka.DriftGuardKafkaStreamsManager;
import ru.eljke.driftguard.spring.metrics.MicrometerDriftDetectionListener;
import ru.eljke.driftguard.spring.metrics.MicrometerKafkaDetectionTelemetryListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot auto-configuration for DriftGuard core components.
 */
@AutoConfiguration
@EnableConfigurationProperties(DriftGuardProperties.class)
@ConditionalOnProperty(prefix = "driftguard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DriftGuardAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DetectorRegistry driftGuardDetectorRegistry(ObjectProvider<DetectorAlgorithm<?, ?>> customAlgorithms) {
        List<DetectorAlgorithm<?, ?>> algorithms = new ArrayList<>(DefaultAlgorithms.all());
        customAlgorithms.orderedStream().forEach(algorithms::add);
        return new SimpleDetectorRegistry(algorithms);
    }

    @Bean
    @ConditionalOnMissingBean
    public DetectorStateStore driftGuardDetectorStateStore() {
        return new InMemoryDetectorStateStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public DetectorRuntimeStateStore driftGuardDetectorRuntimeStateStore(DetectorStateStore detectorStateStore) {
        return new SplitDetectorRuntimeStateStore(detectorStateStore, new InMemoryEmissionStateStore());
    }

    @Bean
    @ConditionalOnMissingBean
    public List<DetectorDefinition> driftGuardDetectorDefinitions(
            DriftGuardProperties properties,
            ObjectProvider<DetectorDefinitionProvider> providers
    ) {
        List<DetectorDefinition> definitions = new ArrayList<>(DetectorDefinitionFactory.create(properties));
        providers.orderedStream()
                .map(DetectorDefinitionProvider::definitions)
                .forEach(definitions::addAll);
        return List.copyOf(definitions);
    }

    @Bean
    @ConditionalOnMissingBean
    public DriftDetectorEngine driftGuardDetectorEngine(
            DetectorRegistry registry,
            DetectorRuntimeStateStore runtimeStateStore,
            List<DetectorDefinition> detectorDefinitions,
            ObjectProvider<DriftDetectionListener> listeners
    ) {
        return new DriftDetectorEngine(
                registry,
                runtimeStateStore,
                detectorDefinitions,
                listeners.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricPointPublisher driftGuardMetricPointPublisher(DriftDetectorEngine engine) {
        return new DriftGuardMetricPointPublisher(engine);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "driftguard.alerts", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DriftAlertMapper driftGuardAlertMapper() {
        return new DefaultDriftAlertMapper();
    }

    @Bean
    @ConditionalOnMissingBean(name = "driftGuardSlf4jAlertSink")
    @ConditionalOnProperty(prefix = "driftguard.alerts", name = {"enabled", "logging-enabled"}, havingValue = "true", matchIfMissing = true)
    public DriftAlertSink driftGuardSlf4jAlertSink() {
        return new Slf4jDriftAlertSink();
    }

    @Bean
    @ConditionalOnMissingBean(name = "driftGuardWebhookAlertSink")
    @ConditionalOnProperty(prefix = "driftguard.alerts.webhook", name = "enabled", havingValue = "true")
    public DriftAlertSink driftGuardWebhookAlertSink(DriftGuardProperties properties) {
        return new WebhookDriftAlertSink(properties.getAlerts().getWebhook());
    }

    @Bean
    @ConditionalOnMissingBean(name = "driftGuardDriftAlertListener")
    @ConditionalOnBean(DriftAlertSink.class)
    @ConditionalOnProperty(prefix = "driftguard.alerts", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DriftDetectionListener driftGuardDriftAlertListener(
            DriftAlertMapper mapper,
            ObjectProvider<DriftAlertSink> alertSinks
    ) {
        return new DriftAlertListener(mapper, alertSinks.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "driftguard.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DriftDetectionListener driftGuardMicrometerDetectionListener(MeterRegistry meterRegistry) {
        return new MicrometerDriftDetectionListener(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "driftguard.metrics", name = "kafka-enabled", havingValue = "true", matchIfMissing = true)
    public KafkaDetectionTelemetryListener driftGuardMicrometerKafkaDetectionTelemetryListener(MeterRegistry meterRegistry) {
        return new MicrometerKafkaDetectionTelemetryListener(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(prefix = "driftguard.micrometer-input", name = "enabled", havingValue = "true")
    public MicrometerMetricInputPoller driftGuardMicrometerMetricInputPoller(
            MeterRegistry meterRegistry,
            MetricPointPublisher publisher,
            DriftGuardProperties properties
    ) {
        return new MicrometerMetricInputPoller(meterRegistry, publisher, properties.getMicrometerInput());
    }

    @Bean
    @ConditionalOnMissingBean(name = "driftGuardDriftEventSinkListener")
    @ConditionalOnBean(DriftEventSink.class)
    public DriftDetectionListener driftGuardDriftEventSinkListener(ObjectProvider<DriftEventSink> sinks) {
        return new DriftEventSinkListener(sinks.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(KafkaStreams.class)
    @ConditionalOnProperty(prefix = "driftguard.kafka", name = "enabled", havingValue = "true")
    public KafkaDriftGuardTopologyConfig driftGuardKafkaTopologyConfig(DriftGuardProperties properties) {
        DriftGuardProperties.KafkaProperties kafka = properties.getKafka();
        DriftGuardKafkaStreamsManager.validate(kafka);
        return new KafkaDriftGuardTopologyConfig(kafka.getInputTopics(), kafka.getOutputTopic());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(KafkaStreams.class)
    @ConditionalOnProperty(prefix = "driftguard.kafka", name = "enabled", havingValue = "true")
    public DriftGuardKafkaStreamsManager driftGuardKafkaStreamsManager(
            DriftGuardProperties properties,
            DriftDetectorEngine engine,
            KafkaDriftGuardTopologyConfig topologyConfig,
            ObjectProvider<KafkaDetectionTelemetryListener> telemetryListeners
    ) {
        DriftGuardProperties.KafkaProperties kafka = properties.getKafka();
        return new DriftGuardKafkaStreamsManager(kafka, () -> new KafkaStreams(
                new KafkaDriftGuardTopologyBuilder(DriftGuardObjectMapper.create(), engine).build(topologyConfig),
                DriftGuardKafkaStreamsManager.streamsProperties(kafka)
        ));
    }
}


