package ru.eljke.driftguard.core.error;

/**
 * Validation error in DriftGuard input, configuration or state.
 */
public final class DriftGuardValidationException extends DriftGuardException {
    public DriftGuardValidationException(ErrorReason reason, Object... args) {
        super(reason, args);
    }

    public DriftGuardValidationException(ErrorReason reason, Throwable cause, Object... args) {
        super(reason, cause, args);
    }
}

