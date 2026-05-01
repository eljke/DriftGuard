package ru.eljke.driftguard.core.error;

import java.util.Objects;

/**
 * Внутренние helper-методы для единообразной валидации core-модели.
 */
public final class DriftGuardErrors {
    private DriftGuardErrors() {
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new DriftGuardValidationException(CoreErrorReason.REQUIRED_VALUE_MISSING, field);
        }
        return value;
    }

    public static String requireNonBlank(String value, String field) {
        requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new DriftGuardValidationException(CoreErrorReason.BLANK_VALUE, field);
        }
        return normalized;
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new DriftGuardValidationException(CoreErrorReason.INVALID_RANGE, message);
        }
    }

    public static DriftGuardValidationException validation(ErrorReason reason, Object... args) {
        return new DriftGuardValidationException(Objects.requireNonNull(reason, "reason must not be null"), args);
    }
}
