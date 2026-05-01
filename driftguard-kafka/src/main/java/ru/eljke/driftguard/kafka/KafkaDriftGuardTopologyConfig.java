package ru.eljke.driftguard.kafka;

import java.util.List;

/**
 * Минимальная конфигурация Kafka Streams topology DriftGuard.
 *
 * @param inputTopics topic-и с входными {@code MetricPoint}
 * @param outputTopic topic для выходных {@code DriftEvent}
 */
public record KafkaDriftGuardTopologyConfig(
        List<String> inputTopics,
        String outputTopic
) {
    public KafkaDriftGuardTopologyConfig {
        inputTopics = List.copyOf(inputTopics == null ? List.of() : inputTopics);
        if (inputTopics.isEmpty()) {
            throw new IllegalArgumentException("inputTopics must not be empty");
        }
        if (outputTopic == null || outputTopic.isBlank()) {
            throw new IllegalArgumentException("outputTopic must not be blank");
        }
        outputTopic = outputTopic.trim();
    }
}
