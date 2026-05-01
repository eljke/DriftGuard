package ru.eljke.driftguard.demo;

import ru.eljke.driftguard.core.error.ErrorReason;

/**
 * Причины ошибок demo API.
 */
public enum DemoErrorReason implements ErrorReason {
    UNKNOWN_SCENARIO("DG-DEMO-001", "Unknown demo scenario: {}"),
    REQUEST_FAILED("DG-DEMO-002", "Demo request failed");

    private final String code;
    private final String description;

    DemoErrorReason(String code, String description) {
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
