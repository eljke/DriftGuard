package ru.eljke.driftguard.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyConfig;

import static org.assertj.core.api.Assertions.assertThat;

class DriftGuardAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DriftGuardAutoConfiguration.class));

    @Test
    void createsCoreEngineByDefault() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(DriftDetectorEngine.class));
    }

    @Test
    void doesNotCreateKafkaTopologyByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(DriftGuardKafkaStreamsManager.class));
    }

    @Test
    void createsKafkaTopologyWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "driftguard.kafka.enabled=true",
                        "driftguard.kafka.auto-startup=false",
                        "driftguard.kafka.bootstrap-servers=localhost:9092",
                        "driftguard.kafka.application-id=test-driftguard",
                        "driftguard.kafka.input-topics[0]=metrics",
                        "driftguard.kafka.output-topic=drift-events"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaDriftGuardTopologyConfig.class);
                    assertThat(context).hasSingleBean(DriftGuardKafkaStreamsManager.class);
                    assertThat(context.getBean(DriftGuardKafkaStreamsManager.class).isAutoStartup()).isFalse();
                });
    }

    @Test
    void failsFastWhenKafkaInputTopicsAreMissing() {
        contextRunner
                .withPropertyValues(
                        "driftguard.kafka.enabled=true",
                        "driftguard.kafka.auto-startup=false",
                        "driftguard.kafka.output-topic=drift-events"
                )
                .run(context -> assertThat(context).hasFailed());
    }
}
