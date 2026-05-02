package ru.eljke.driftguard.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.eljke.driftguard.core.domain.DriftDirection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Настройки DriftGuard, читаемые из {@code application.yml}.
 *
 * <p>Класс является публичным configuration contract-ом starter-а. Все поля
 * используют Spring Boot relaxed binding: например {@code baselineWindowSize}
 * задаётся как {@code baseline-window-size}. Если список {@link #detectors}
 * пустой, starter создаёт небольшой набор detector-ов по умолчанию для
 * demo/quick-start режима.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "driftguard")
public class DriftGuardProperties {
    /**
     * Включает auto-configuration DriftGuard.
     *
     * <p>При {@code false} starter не создаёт registry, state store, detector
     * definitions и {@code DriftDetectorEngine}.</p>
     */
    private boolean enabled = true;

    /**
     * Набор detector definitions, применяемых к входящим {@code MetricPoint}.
     *
     * <p>Каждый элемент задаёт алгоритм, фильтр потоков метрик, окна, пороги и
     * политику публикации событий.</p>
     */
    private List<DetectorProperties> detectors = new ArrayList<>();

    /**
     * Глобальный флаг включения detector definitions из application.yml.
     */
    private boolean detectorsEnabled = true;

    /**
     * Настройки runtime-метрик DriftGuard.
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * Настройки Kafka Streams adapter-а.
     *
     * <p>Kafka-часть выключена по умолчанию, потому что не каждому Spring
     * приложению нужен transport adapter. Если включить {@code kafka.enabled},
     * starter создаст и запустит Kafka Streams topology, которая читает
     * {@code MetricPoint} из input topic-ов и пишет {@code DriftEvent} в output
     * topic.</p>
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
         * Позволяет временно выключить detector без удаления его конфигурации.
         */
        private boolean enabled = true;

        /**
         * Имя detector-а в {@code DriftEvent.detector} и ключе состояния.
         */
        private String name;

        /**
         * Имя алгоритма: {@code page-hinkley}, {@code adwin}, {@code psi},
         * {@code ks} или {@code chi-square}.
         */
        private String algorithm;

        /**
         * Разрешённые значения {@code MetricKey.service}. Пустой список
         * означает, что detector применяется к любому сервису.
         */
        private List<String> services = new ArrayList<>();

        /**
         * Разрешённые значения {@code MetricKey.metric}. Пустой список
         * означает, что detector применяется к любой метрике.
         */
        private List<String> metrics = new ArrayList<>();

        /**
         * Разрешённые значения {@code MetricKey.operation}. Пустой список
         * означает, что detector применяется к любой операции.
         */
        private List<String> operations = new ArrayList<>();

        /**
         * Разрешённые значения {@code MetricKey.instance}. Пустой список
         * означает, что detector применяется к любому instance id.
         */
        private List<String> instances = new ArrayList<>();

        /**
         * Размер baseline-окна для PSI, KS и chi-square.
         */
        private int baselineWindowSize = 40;

        /**
         * Размер current-окна для PSI, KS и chi-square.
         */
        private int currentWindowSize = 40;

        /**
         * Размер адаптивного окна для ADWIN.
         */
        private int windowSize = 40;

        /**
         * Минимальный размер каждой части окна при поиске разреза в ADWIN.
         */
        private int minSubWindowSize = 10;

        /**
         * Число первых samples, используемых Page-Hinkley для прогрева baseline.
         */
        private int warmupSamples = 20;

        /**
         * Количество bucket-ов для PSI и chi-square.
         */
        private int buckets = 5;

        /**
         * Warning-порог для PSI или Page-Hinkley.
         */
        private double warningThreshold = 0.1;

        /**
         * Critical-порог для PSI или Page-Hinkley.
         */
        private double criticalThreshold = 0.25;

        /**
         * P-value граница warning-события для KS и chi-square.
         */
        private double warningPValue = 0.05;

        /**
         * P-value граница critical-события для KS и chi-square.
         */
        private double criticalPValue = 0.01;

        /**
         * Sensitivity/confidence параметр для ADWIN и Page-Hinkley.
         */
        private double delta = 0.1;

        /**
         * Скорость адаптации среднего в Page-Hinkley.
         */
        private double alpha = 0.05;

        /**
         * Направление сдвига среднего, которое ищет Page-Hinkley.
         *
         * <p>{@code UP} подходит для latency, error-rate и queue-size.
         * {@code DOWN} подходит для throughput и других метрик, где опасно падение.</p>
         */
        private DriftDirection direction = DriftDirection.UP;

        /**
         * Сглаживание bucket-ов в PSI, защищающее от деления на ноль.
         */
        private double epsilon = 1e-4;

        /**
         * Множитель score, после которого ADWIN создаёт critical-событие.
         */
        private double criticalMultiplier = 2.0;

        /**
         * Минимальная ожидаемая частота bucket-а для chi-square.
         */
        private double minExpectedCount = 1.0;

        /**
         * Политика публикации событий поверх сырых сигналов алгоритма.
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
         * Сколько последовательных сигналов алгоритма требуется перед
         * публикацией {@code DriftEvent}.
         */
        private int minConsecutiveSignals = 1;

        /**
         * Минимальная пауза между опубликованными событиями одного detector-а
         * для одного потока метрик.
         */
        private Duration cooldown = Duration.ZERO;

        /**
         * Сколько нормальных точек подряд должно прийти, чтобы текущий drift
         * episode считался завершённым и detector снова мог публиковать событие.
         */
        private int recoveryConsecutiveNormal = 1;
    }

    @Getter
    @Setter
    public static class MetricsProperties {
        /**
         * Включает Micrometer listener для runtime-метрик detection pipeline.
         *
         * <p>Listener создаётся только если в приложении есть {@code MeterRegistry}.
         * Например, в Spring Boot приложении его обычно добавляет Actuator.</p>
         */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class KafkaProperties {
        /**
         * Включает Kafka Streams topology DriftGuard.
         */
        private boolean enabled = false;

        /**
         * Kafka bootstrap servers, например {@code localhost:9092}.
         */
        private String bootstrapServers = "localhost:9092";

        /**
         * Kafka Streams application id.
         */
        private String applicationId = "driftguard";

        /**
         * Topic-и с JSON {@code MetricPoint}.
         */
        private List<String> inputTopics = new ArrayList<>();

        /**
         * Topic, куда topology пишет JSON {@code DriftEvent}.
         */
        private String outputTopic = "driftguard.drift-events";

        /**
         * Локальная директория state store-ов Kafka Streams. Если пусто,
         * используется значение Kafka Streams по умолчанию.
         */
        private String stateDir;

        /**
         * Запускать ли topology автоматически при старте Spring context-а.
         */
        private boolean autoStartup = true;

        public void setInputTopics(List<String> inputTopics) {
            this.inputTopics = inputTopics == null ? new ArrayList<>() : inputTopics;
        }
    }
}
