package ru.eljke.driftguard.core.alert;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.LinkedHashMap;
import java.util.Map;

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
        String message = "%s: current %.4f vs baseline %.4f, score %.4f, detector %s (%s). %s"
                .formatted(
                        title,
                        event.currentValue(),
                        event.baselineValue(),
                        event.score(),
                        event.detector(),
                        event.algorithm(),
                        event.reason()
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
}
