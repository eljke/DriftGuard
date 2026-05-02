package ru.eljke.driftguard.algorithms.support;

import ru.eljke.driftguard.core.detector.DetectionContext;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.Map;
import java.util.LinkedHashMap;

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

    public static Map<String, Object> standardDetails(
            double baselineMean,
            double currentMean,
            double warningThreshold,
            double criticalThreshold,
            Map<String, Object> algorithmDetails
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("baselineMean", baselineMean);
        details.put("currentMean", currentMean);
        details.put("absoluteChange", currentMean - baselineMean);
        details.put("relativeChangePercent", relativeChangePercent(baselineMean, currentMean));
        details.put("warningThreshold", warningThreshold);
        details.put("criticalThreshold", criticalThreshold);
        if (algorithmDetails != null) {
            details.putAll(algorithmDetails);
        }
        return Map.copyOf(details);
    }

    private static double relativeChangePercent(double baseline, double current) {
        return Math.abs(baseline) < 1.0e-9 ? 0.0 : ((current - baseline) / Math.abs(baseline)) * 100.0;
    }

    public static DriftSeverity severity(double score, double warningThreshold, double criticalThreshold) {
        return score >= criticalThreshold ? DriftSeverity.CRITICAL : DriftSeverity.WARNING;
    }
}
