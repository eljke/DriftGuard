package ru.eljke.driftguard.algorithms.support;

import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Map;

/**
 * Общий helper для единообразного создания drift events из кода алгоритмов.
 */
public final class DriftEvents {
    private DriftEvents() {
    }

    public static DriftEvent create(
            MetricPoint point,
            DetectionContext context,
            String algorithm,
            DriftDirection direction,
            DriftSeverity severity,
            double score,
            double currentValue,
            double baselineValue,
            String reason,
            Map<String, Object> details
    ) {
        return new DriftEvent(
                null,
                point.key(),
                point.timestamp(),
                point.timestamp(),
                point.timestamp(),
                direction,
                severity,
                score,
                currentValue,
                baselineValue,
                context.detectorName(),
                algorithm,
                reason,
                point.tags(),
                details
        );
    }

    public static DriftSeverity severity(double score, double warningThreshold, double criticalThreshold) {
        return score >= criticalThreshold ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
    }
}
