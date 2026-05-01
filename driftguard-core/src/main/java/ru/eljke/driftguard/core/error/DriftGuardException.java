package ru.eljke.driftguard.core.error;

import java.util.Objects;

/**
 * Базовое unchecked-исключение DriftGuard с типизированной причиной.
 */
public class DriftGuardException extends RuntimeException {
    private final ErrorReason reason;

    public DriftGuardException(ErrorReason reason, Object... args) {
        super(Objects.requireNonNull(reason, "reason must not be null").format(args));
        this.reason = reason;
    }

    public DriftGuardException(ErrorReason reason, Throwable cause, Object... args) {
        super(Objects.requireNonNull(reason, "reason must not be null").format(args), cause);
        this.reason = reason;
    }

    public ErrorReason reason() {
        return reason;
    }

    public String code() {
        return reason.code();
    }
}
