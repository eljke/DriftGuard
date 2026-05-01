package ru.eljke.driftguard.spring;

import ru.eljke.driftguard.core.error.ErrorReason;

/**
 * Причины ошибок Spring Boot starter-а.
 */
public enum SpringDriftGuardErrorReason implements ErrorReason {
    UNSUPPORTED_ALGORITHM("DG-SPRING-001", "Unsupported DriftGuard algorithm: {}"),
    REQUIRED_PROPERTY("DG-SPRING-002", "{} must not be blank"),
    EMPTY_KAFKA_INPUT_TOPICS("DG-SPRING-003", "driftguard.kafka.input-topics must not be empty"),
    BLANK_KAFKA_OUTPUT_TOPIC("DG-SPRING-004", "driftguard.kafka.output-topic must not be blank"),
    BLANK_KAFKA_BOOTSTRAP_SERVERS("DG-SPRING-005", "driftguard.kafka.bootstrap-servers must not be blank"),
    BLANK_KAFKA_APPLICATION_ID("DG-SPRING-006", "driftguard.kafka.application-id must not be blank");

    private final String code;
    private final String description;

    SpringDriftGuardErrorReason(String code, String description) {
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
