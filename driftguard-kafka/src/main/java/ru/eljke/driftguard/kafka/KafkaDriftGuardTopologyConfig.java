package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.error.DriftGuardValidationException;

import java.util.List;

/**
 * Минимальная конфигурация Kafka Streams topology DriftGuard.
 *
 * @param inputTopics topic-и с входными {@code MetricPoint}
 * @param outputTopic topic для выходных {@code DriftEvent}
 * @param detectionErrorTopic опциональный topic для ошибок detector-а
 */
public record KafkaDriftGuardTopologyConfig(
        List<String> inputTopics,
        String outputTopic,
        String detectionErrorTopic
) {
    public KafkaDriftGuardTopologyConfig(List<String> inputTopics, String outputTopic) {
        this(inputTopics, outputTopic, null);
    }

    public KafkaDriftGuardTopologyConfig {
        inputTopics = List.copyOf(inputTopics == null ? List.of() : inputTopics);
        if (inputTopics.isEmpty()) {
            throw new DriftGuardValidationException(KafkaDriftGuardErrorReason.EMPTY_INPUT_TOPICS);
        }
        if (outputTopic == null || outputTopic.isBlank()) {
            throw new DriftGuardValidationException(KafkaDriftGuardErrorReason.BLANK_OUTPUT_TOPIC);
        }
        outputTopic = outputTopic.trim();
        if (detectionErrorTopic != null) {
            detectionErrorTopic = detectionErrorTopic.trim();
            if (detectionErrorTopic.isBlank()) {
                detectionErrorTopic = null;
            }
        }
    }
}

