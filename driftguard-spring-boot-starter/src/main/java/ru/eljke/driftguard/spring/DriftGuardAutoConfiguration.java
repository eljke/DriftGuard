package ru.eljke.driftguard.spring;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.eljke.driftguard.algorithms.DefaultAlgorithms;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.state.DetectorStateStore;
import ru.eljke.driftguard.core.state.InMemoryDetectorStateStore;
import ru.eljke.driftguard.kafka.DriftGuardObjectMapper;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyBuilder;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyConfig;

import java.util.List;

/**
 * Spring Boot auto-configuration для core-компонентов DriftGuard.
 */
@AutoConfiguration
@EnableConfigurationProperties(DriftGuardProperties.class)
@ConditionalOnProperty(prefix = "driftguard", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DriftGuardAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DetectorRegistry driftGuardDetectorRegistry() {
        return DefaultAlgorithms.registry();
    }

    @Bean
    @ConditionalOnMissingBean
    public DetectorStateStore driftGuardDetectorStateStore() {
        return new InMemoryDetectorStateStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public List<DetectorDefinition> driftGuardDetectorDefinitions(DriftGuardProperties properties) {
        return DetectorDefinitionFactory.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DriftDetectorEngine driftGuardDetectorEngine(
            DetectorRegistry registry,
            DetectorStateStore stateStore,
            List<DetectorDefinition> detectorDefinitions
    ) {
        return new DriftDetectorEngine(registry, stateStore, detectorDefinitions);
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
            KafkaDriftGuardTopologyConfig topologyConfig
    ) {
        DriftGuardProperties.KafkaProperties kafka = properties.getKafka();
        return new DriftGuardKafkaStreamsManager(kafka, () -> new KafkaStreams(
                new KafkaDriftGuardTopologyBuilder(DriftGuardObjectMapper.create(), engine).build(topologyConfig),
                DriftGuardKafkaStreamsManager.streamsProperties(kafka)
        ));
    }
}
