package ru.eljke.driftguard.core.alert;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default event-to-alert mapper used when an application does not provide a
 * custom template.
 */
public final class DefaultDriftAlertMapper implements DriftAlertMapper {
    @Override
    public DriftAlert map(DriftEvent event) {
        MetricKey key = event.key();
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("service", key.service());
        labels.put("metric", key.metric());
        key.operationValue().ifPresent(operation -> labels.put("operation", operation));
        key.instanceValue().ifPresent(instance -> labels.put("instance", instance));
        labels.put("detector", event.detector());
        labels.put("algorithm", event.algorithm());
        labels.put("phase", event.phase().name());
        labels.put("severity", event.severity().name());

        String stream = key.operationValue()
                .map(operation -> key.service() + "/" + operation + "/" + key.metric())
                .orElse(key.service() + "/" + key.metric());
        String title = "%s drift %s on %s".formatted(event.severity(), event.phase(), stream);
        String evidence = evidence(event.details());
        String message = String.format(
                        Locale.ROOT,
                        "%s: current %.4f vs baseline %.4f, score %.4f, detector %s (%s). %s%s",
                        title,
                        event.currentValue(),
                        event.baselineValue(),
                        event.score(),
                        event.detector(),
                        event.algorithm(),
                        event.reason(),
                        evidence.isBlank() ? "" : " Evidence: " + evidence
                );

        return DriftAlert.builder()
                .id(event.id())
                .event(event)
                .title(title)
                .message(message)
                .severity(event.severity())
                .key(key)
                .occurredAt(event.detectedAt())
                .labels(labels)
                .build();
    }

    private static String evidence(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        return details.entrySet().stream()
                .filter(entry -> importantDetail(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + format(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static boolean importantDetail(String key) {
        return switch (key) {
            case "relativeChangePercent",
                 "warningThreshold",
                 "criticalThreshold",
                 "pValue",
                 "statistic",
                 "chiSquare",
                 "count",
                 "windowSize",
                 "split",
                 "epsilon",
                 "meanDifference",
                 "scoreMultiplier",
                 "consecutiveSignals",
                 "recoveryConsecutiveNormal" -> true;
            default -> false;
        };
    }

    private static String format(Object value) {
        if (value instanceof Number number) {
            return String.format(Locale.ROOT, "%.4f", number.doubleValue());
        }
        return String.valueOf(value);
    }
}
