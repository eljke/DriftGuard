package ru.eljke.driftguard.kafka;

import ru.eljke.driftguard.core.error.ErrorReason;

/**
 * Причины ошибок Kafka adapter-а.
 */
public enum KafkaDriftGuardErrorReason implements ErrorReason {
    EMPTY_INPUT_TOPICS("DG-KAFKA-001", "inputTopics must not be empty"),
    BLANK_OUTPUT_TOPIC("DG-KAFKA-002", "outputTopic must not be blank"),
    JSON_SERIALIZATION_FAILED("DG-KAFKA-003", "Failed to serialize {} as JSON"),
    JSON_DESERIALIZATION_FAILED("DG-KAFKA-004", "Failed to deserialize JSON as {}");

    private final String code;
    private final String description;

    KafkaDriftGuardErrorReason(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String description() {
        return description;
    }
}
