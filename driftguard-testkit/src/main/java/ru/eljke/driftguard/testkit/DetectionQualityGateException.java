package ru.eljke.driftguard.testkit;

/**
 * Ошибка quality gate, которую удобно использовать в unit/CI тестах.
 */
public final class DetectionQualityGateException extends AssertionError {
    private final DetectionQualityReport report;

    public DetectionQualityGateException(DetectionQualityReport report) {
        super(report.describe());
        this.report = report;
    }

    public DetectionQualityReport report() {
        return report;
    }
}
