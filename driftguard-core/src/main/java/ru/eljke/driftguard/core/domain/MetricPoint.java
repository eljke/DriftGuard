package ru.eljke.driftguard.core.domain;

import lombok.Builder;
import ru.eljke.driftguard.core.error.CoreErrorReason;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.Map;

/**
 * One observed value in a technical metric stream.
 *
 * <p>This is the main input contract of the core module. The type intentionally contains no
 * Kafka-, Spring-, JSON- or Prometheus-specific fields. Adapters should
 * convert their native event format to this record before calling
 * detection engine.</p>
 *
 * @param key stable metric identity for routing and state lookup
 * @param timestamp event timestamp of the observed value
 * @param value numeric metric value; must be finite
 * @param kind semantic metric kind
 * @param tags indexed string dimensions for filtering and event output
 * @param attributes additional non-indexed payload for adapters and diagnostics
 */
@Builder(toBuilder = true)
public record MetricPoint(
        MetricKey key,
        Instant timestamp,
        double value,
        MetricKind kind,
        Map<String, String> tags,
        Map<String, Object> attributes
) {
    public MetricPoint {
        key = DriftGuardErrors.requireNonNull(key, "key");
        timestamp = DriftGuardErrors.requireNonNull(timestamp, "timestamp");
        kind = DriftGuardErrors.requireNonNull(kind, "kind");
        if (!Double.isFinite(value)) {
            throw DriftGuardErrors.validation(CoreErrorReason.NON_FINITE_VALUE, "value");
        }
        tags = Map.copyOf(tags == null ? Map.of() : tags);
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
    }

    public static MetricPoint gauge(MetricKey key, Instant timestamp, double value) {
        return new MetricPoint(key, timestamp, value, MetricKind.GAUGE, Map.of(), Map.of());
    }

    public MetricPoint observedAt(Instant observedAt) {
        return new MetricPoint(key, observedAt, value, kind, tags, attributes);
    }
}

