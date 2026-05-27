package ru.eljke.driftguard.testkit.quality;

/**
 * Single failed quality-gate assertion.
 *
 * @param metric quality metric name
 * @param expected required threshold value
 * @param actual observed metric value
 * @param direction comparison direction for the threshold
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


