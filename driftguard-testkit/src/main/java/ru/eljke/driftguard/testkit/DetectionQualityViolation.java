package ru.eljke.driftguard.testkit;

/**
 * Одно нарушение quality gate.
 *
 * @param metric имя проверяемой метрики
 * @param expected ожидаемое пороговое значение
 * @param actual фактическое значение
 * @param direction направление нарушения
 */
public record DetectionQualityViolation(
        String metric,
        Object expected,
        Object actual,
        Direction direction
) {
    public enum Direction {
        BELOW_MINIMUM,
        ABOVE_MAXIMUM
    }

    public static DetectionQualityViolation below(String metric, Object expected, Object actual) {
        return new DetectionQualityViolation(metric, expected, actual, Direction.BELOW_MINIMUM);
    }

    public static DetectionQualityViolation above(String metric, Object expected, Object actual) {
        return new DetectionQualityViolation(metric, expected, actual, Direction.ABOVE_MAXIMUM);
    }

    public String describe() {
        return switch (direction) {
            case BELOW_MINIMUM -> metric + " expected >= " + expected + ", actual " + actual;
            case ABOVE_MAXIMUM -> metric + " expected <= " + expected + ", actual " + actual;
        };
    }
}
