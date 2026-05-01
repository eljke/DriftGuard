package ru.eljke.driftguard.core.error;

/**
 * Причины ошибок core-модуля.
 */
public enum CoreErrorReason implements ErrorReason {
    REQUIRED_VALUE_MISSING("DG-CORE-001", "{} must not be null"),
    BLANK_VALUE("DG-CORE-002", "{} must not be blank"),
    INVALID_RANGE("DG-CORE-003", "{}"),
    NON_FINITE_VALUE("DG-CORE-004", "{} must be finite"),
    DUPLICATE_ALGORITHM("DG-CORE-005", "Duplicate detector algorithm: {}"),
    UNKNOWN_ALGORITHM("DG-CORE-006", "Unknown detector algorithm: {}"),
    STATE_ALGORITHM_MISMATCH("DG-CORE-007", "Stored state algorithm mismatch for {}");

    private final String code;
    private final String description;

    CoreErrorReason(String code, String description) {
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
