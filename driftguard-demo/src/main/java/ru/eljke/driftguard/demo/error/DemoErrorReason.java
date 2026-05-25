package ru.eljke.driftguard.demo.error;

import ru.eljke.driftguard.core.error.ErrorReason;

/**
 * English demo documentation.
 */
public enum DemoErrorReason implements ErrorReason {
    UNKNOWN_SCENARIO("DG-DEMO-001", "Unknown demo scenario: {}"),
    REQUEST_FAILED("DG-DEMO-002", "Demo request failed"),
    KAFKA_DEMO_DISABLED("DG-DEMO-003", "Kafka demo is disabled"),
    KAFKA_DEMO_FAILED("DG-DEMO-004", "Kafka demo failed: {}"),
    UNKNOWN_PROFILE("DG-DEMO-005", "Unknown detector profile: {}");

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


