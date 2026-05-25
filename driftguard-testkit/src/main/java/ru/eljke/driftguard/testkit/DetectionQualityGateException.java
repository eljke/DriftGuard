package ru.eljke.driftguard.testkit;

/**
 * English API documentation.
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


