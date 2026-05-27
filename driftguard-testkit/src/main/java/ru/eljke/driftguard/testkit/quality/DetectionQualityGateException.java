package ru.eljke.driftguard.testkit.quality;

/**
 * Exception thrown when a benchmark report violates a quality gate.
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


