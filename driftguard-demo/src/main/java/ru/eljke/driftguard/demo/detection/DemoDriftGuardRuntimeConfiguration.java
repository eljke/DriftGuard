package ru.eljke.driftguard.demo.detection;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eljke.driftguard.kafka.DriftGuardObjectMapper;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyBuilder;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyConfig;
import ru.eljke.driftguard.spring.DriftGuardKafkaStreamsManager;
import ru.eljke.driftguard.spring.DriftGuardProperties;

/**
 * English demo documentation.
 */
@Configuration
public class DemoDriftGuardRuntimeConfiguration {
    @Bean
    @ConditionalOnClass(KafkaStreams.class)
    @ConditionalOnProperty(prefix = "driftguard.kafka", name = "enabled", havingValue = "true")
    public DriftGuardKafkaStreamsManager driftGuardKafkaStreamsManager(
            DriftGuardProperties properties,
            KafkaDriftGuardTopologyConfig topologyConfig,
            DemoDetectionRuntime runtime
    ) {
        DriftGuardProperties.KafkaProperties kafka = properties.getKafka();
        return new DriftGuardKafkaStreamsManager(kafka, () -> new KafkaStreams(
                new KafkaDriftGuardTopologyBuilder(DriftGuardObjectMapper.create(), runtime::detect).build(topologyConfig),
                DriftGuardKafkaStreamsManager.streamsProperties(kafka)
        ));
    }
}


