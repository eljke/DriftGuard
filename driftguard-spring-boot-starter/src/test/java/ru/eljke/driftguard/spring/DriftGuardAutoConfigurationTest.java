package ru.eljke.driftguard.spring;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eljke.driftguard.core.config.DetectorConfig;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.core.config.DetectorDefinitionProvider;
import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.detector.DetectionResult;
import ru.eljke.driftguard.core.detector.DetectorAlgorithm;
import ru.eljke.driftguard.core.detector.DetectorRegistry;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.DriftDetectorEngine;
import ru.eljke.driftguard.core.domain.MetricPoint;
import ru.eljke.driftguard.kafka.KafkaDetectionTelemetryListener;
import ru.eljke.driftguard.kafka.KafkaDriftGuardTopologyConfig;

import java.util.List;

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
    void registersCustomDetectorAlgorithmBeans() {
        contextRunner
                .withUserConfiguration(CustomAlgorithmConfiguration.class)
                .run(context -> {
                    DetectorRegistry registry = context.getBean(DetectorRegistry.class);

                    assertThat(registry.algorithmNames()).contains("page-hinkley", CustomConfig.ALGORITHM);
                    assertThat(registry.find(CustomConfig.ALGORITHM)).isPresent();
                });
    }


    @Test
    void registersKafkaMicrometerTelemetryListenerWhenMeterRegistryExists() {
        contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(KafkaDetectionTelemetryListener.class);
                    assertThat(context.getBean(KafkaDetectionTelemetryListener.class))
                            .isInstanceOf(MicrometerKafkaDetectionTelemetryListener.class);
                });
    }

    @Test
    void appendsCustomDetectorDefinitionProviders() {
        contextRunner
                .withUserConfiguration(CustomDefinitionConfiguration.class)
                .run(context -> {
                    @SuppressWarnings("unchecked")
                    List<DetectorDefinition> definitions = (List<DetectorDefinition>) context.getBean("driftGuardDetectorDefinitions");

                    assertThat(definitions)
                            .extracting(DetectorDefinition::name)
                            .contains("custom-latency-detector");
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


    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryConfiguration {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAlgorithmConfiguration {
        @Bean
        DetectorAlgorithm<CustomConfig, CustomState> customDetectorAlgorithm() {
            return new CustomDetectorAlgorithm();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDefinitionConfiguration {
        @Bean
        DetectorDefinitionProvider customDetectorDefinitionProvider() {
            return () -> List.of(new DetectorDefinition(
                    "custom-latency-detector",
                    new CustomConfig(),
                    key -> key.metric().equals("latency")
            ));
        }
    }

    record CustomConfig() implements DetectorConfig {
        static final String ALGORITHM = "custom-threshold";

        @Override
        public String algorithm() {
            return ALGORITHM;
        }
    }

    record CustomState() implements DetectorState {
        @Override
        public String algorithm() {
            return CustomConfig.ALGORITHM;
        }
    }

    static class CustomDetectorAlgorithm implements DetectorAlgorithm<CustomConfig, CustomState> {
        @Override
        public String name() {
            return CustomConfig.ALGORITHM;
        }

        @Override
        public Class<CustomConfig> configType() {
            return CustomConfig.class;
        }

        @Override
        public CustomState initialState(CustomConfig config) {
            return new CustomState();
        }

        @Override
        public DetectionResult<CustomState> detect(
                MetricPoint point,
                CustomState state,
                CustomConfig config,
                DetectionContext context
        ) {
            return DetectionResult.noDrift(state);
        }
    }
}
