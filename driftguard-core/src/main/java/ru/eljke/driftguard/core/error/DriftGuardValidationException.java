package ru.eljke.driftguard.core.error;

/**
 * Ошибка валидации входных данных, конфигурации или состояния DriftGuard.
 */
public final class DriftGuardValidationException extends DriftGuardException {
    public DriftGuardValidationException(ErrorReason reason, Object... args) {
        super(reason, args);
    }

    public DriftGuardValidationException(ErrorReason reason, Throwable cause, Object... args) {
        super(reason, cause, args);
    }
}
