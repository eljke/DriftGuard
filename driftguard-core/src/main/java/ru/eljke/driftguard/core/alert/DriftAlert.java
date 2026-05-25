package ru.eljke.driftguard.core.alert;

import lombok.Builder;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.Map;

/**
 * User-facing alert generated from a public {@link DriftEvent}.
 *
 * <p>The alert keeps the original event for integrations that need full
 * evidence, but exposes title and message fields for simple sinks such as logs,
 * chat bots, email or incident-management tools.</p>
 *
 * @param id alert id, usually equal to the source event id
 * @param event source drift event
 * @param title compact human-readable alert title
 * @param message detailed human-readable alert body
 * @param severity alert severity
 * @param key metric stream that drifted
 * @param occurredAt alert timestamp
 * @param labels stable labels for routing and templates
 */
@Builder(toBuilder = true)
public record DriftAlert(
        String id,
        DriftEvent event,
        String title,
        String message,
        DriftSeverity severity,
        MetricKey key,
        Instant occurredAt,
        Map<String, String> labels
) {
    public DriftAlert {
        event = DriftGuardErrors.requireNonNull(event, "event");
        id = id == null || id.isBlank() ? event.id() : id;
        title = normalize(title, "title");
        message = normalize(message, "message");
        severity = severity == null ? event.severity() : severity;
        key = key == null ? event.key() : key;
        occurredAt = occurredAt == null ? event.detectedAt() : occurredAt;
        labels = Map.copyOf(labels == null ? Map.of() : labels);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
