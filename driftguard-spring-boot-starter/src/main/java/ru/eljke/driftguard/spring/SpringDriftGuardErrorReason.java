package ru.eljke.driftguard.spring;

import ru.eljke.driftguard.core.error.ErrorReason;

/**
 * Причины ошибок Spring Boot starter-а.
 */
public enum SpringDriftGuardErrorReason implements ErrorReason {
    UNSUPPORTED_ALGORITHM("DG-SPRING-001", "Unsupported DriftGuard algorithm: {}"),
    REQUIRED_PROPERTY("DG-SPRING-002", "{} must not be blank");

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
