package ru.eljke.driftguard.demo.config;

import java.time.Duration;
import java.util.List;

/**
 * Снимок runtime-конфигурации demo-приложения, который UI показывает без
 * прямого чтения {@code application.yml}.
 *
 * @param aggressiveness агрегированная оценка чувствительности detector-ов
 * @param kafka настройки Kafka demo и Kafka Streams adapter-а
 * @param detectors detector definitions, активные в текущем Spring context-е
 */
public record DemoConfigurationView(
        AggressivenessView aggressiveness,
        KafkaConfigurationView kafka,
        List<DetectorConfigurationView> detectors
) {
    /**
     * UI-представление текущей чувствительности.
     *
     * @param level человекочитаемый уровень чувствительности
     * @param description пояснение, как интерпретировать уровень
     */
    public record AggressivenessView(String level, String description) {
    }

    /**
     * Kafka-настройки, важные для демонстрационного контура.
     *
     * @param demoEnabled включён ли Kafka demo endpoint
     * @param bootstrapServers Kafka bootstrap servers
     * @param inputTopic topic входных metric points
     * @param outputTopic topic найденных drift events
     * @param applicationId Kafka Streams application id
     * @param playbackInterval интервал публикации synthetic points
     */
    public record KafkaConfigurationView(
            boolean demoEnabled,
            String bootstrapServers,
            String inputTopic,
            String outputTopic,
            String applicationId,
            Duration playbackInterval
    ) {
    }

    /**
     * Конфигурация одного detector-а из {@code driftguard.detectors}.
     *
     * @param name имя detector-а
     * @param algorithm алгоритм detector-а
     * @param services фильтр по сервисам
     * @param metrics фильтр по метрикам
     * @param warningThreshold warning-порог score
     * @param criticalThreshold critical-порог score
     * @param warningPValue warning-порог p-value
     * @param criticalPValue critical-порог p-value
     * @param warmupSamples число samples для прогрева
     * @param emissionPolicy политика публикации событий
     * @param sensitivity оценка чувствительности конкретного detector-а
     */
    public record DetectorConfigurationView(
            String name,
            String algorithm,
            List<String> services,
            List<String> metrics,
            double warningThreshold,
            double criticalThreshold,
            double warningPValue,
            double criticalPValue,
            int warmupSamples,
            EmissionPolicyView emissionPolicy,
            String sensitivity
    ) {
    }

    /**
     * Runtime-представление политики подавления повторяющихся событий.
     *
     * @param minConsecutiveSignals сколько подряд сигналов нужно перед событием
     * @param cooldown минимальная пауза между событиями одного detector-а
     */
    public record EmissionPolicyView(int minConsecutiveSignals, Duration cooldown) {
    }
}
