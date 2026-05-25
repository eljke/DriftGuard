package ru.eljke.driftguard.core.domain;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

/**
 * Public event created when a detector concludes that a metric stream has drifted
 * .
 *
 * <p>The event contains a compact shared schema and extensible map fields. Shared
 * fields fit Kafka topics, REST APIs, logs and UI tables.
 * Algorithm-specific evidence should be placed in {@link #details()}.</p>
 *
 * @param id unique event id; generated automatically when omitted
 * @param key metric stream where drift was detected
 * @param detectedAt time when the detector generated the event
 * @param windowStart start of the analyzed window, when known
 * @param windowEnd end of the analyzed window, when known
 * @param phase drift episode lifecycle phase
 * @param direction drift direction or type
 * @param severity severity level computed from configured thresholds
 * @param score algorithm-specific drift score
 * @param currentValue representative current value
 * @param baselineValue representative baseline value
 * @param detector configured detector instance name
 * @param algorithm algorithm implementation name
 * @param reason short human-readable explanation
 * @param tags copied or enriched metric tags
 * @param details algorithm-specific details and evidence
 */
public record DriftEvent(
        String id,
        MetricKey key,
        Instant detectedAt,
        Instant windowStart,
        Instant windowEnd,
        DriftEventPhase phase,
        DriftDirection direction,
        DriftSeverity severity,
        double score,
        double currentValue,
        double baselineValue,
        String detector,
        String algorithm,
        String reason,
        Map<String, String> tags,
        Map<String, Object> details
) {
    public DriftEvent(
            String id,
            MetricKey key,
            Instant detectedAt,
            Instant windowStart,
            Instant windowEnd,
            DriftDirection direction,
            DriftSeverity severity,
            double score,
            double currentValue,
            double baselineValue,
            String detector,
            String algorithm,
            String reason,
            Map<String, String> tags,
            Map<String, Object> details
    ) {
        this(id, key, detectedAt, windowStart, windowEnd, DriftEventPhase.STARTED, direction, severity, score,
                currentValue, baselineValue, detector, algorithm, reason, tags, details);
    }

    public DriftEvent {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        key = DriftGuardErrors.requireNonNull(key, "key");
        detectedAt = DriftGuardErrors.requireNonNull(detectedAt, "detectedAt");
        windowStart = windowStart == null ? detectedAt : windowStart;
        windowEnd = windowEnd == null ? detectedAt : windowEnd;
        phase = phase == null ? DriftEventPhase.STARTED : phase;
        direction = direction == null ? DriftDirection.UNKNOWN : direction;
        severity = severity == null ? DriftSeverity.WARNING : severity;
        detector = normalize(detector, "detector");
        algorithm = normalize(algorithm, "algorithm");
        reason = reason == null ? "" : reason;
        tags = Map.copyOf(tags == null ? Map.of() : tags);
        details = Map.copyOf(details == null ? Map.of() : details);
    }

    public DriftEvent ongoingAt(Instant observedAt, int consecutiveSignals) {
        return ongoingAt(observedAt, consecutiveSignals, baselineValue);
    }

    public DriftEvent ongoingAt(Instant observedAt, int consecutiveSignals, double episodeBaselineValue) {
        Map<String, Object> ongoingDetails = new LinkedHashMap<>(details);
        ongoingDetails.put("consecutiveSignals", consecutiveSignals);
        ongoingDetails.put("episodeOngoing", true);
        ongoingDetails.put("episodeBaselineValue", episodeBaselineValue);
        return new DriftEvent(
                null,
                key,
                observedAt,
                windowStart,
                observedAt,
                DriftEventPhase.ONGOING,
                direction,
                severity,
                score,
                currentValue,
                episodeBaselineValue,
                detector,
                algorithm,
                reason,
                tags,
                ongoingDetails
        );
    }

    public DriftEvent recoveredAt(Instant recoveredAt, int recoveryConsecutiveNormal) {
        return recoveredAt(recoveredAt, recoveryConsecutiveNormal, currentValue);
    }

    public DriftEvent recoveredAt(Instant recoveredAt, int recoveryConsecutiveNormal, double recoveredValue) {
        Map<String, Object> recoveryDetails = new LinkedHashMap<>(details);
        recoveryDetails.put("recoveryConsecutiveNormal", recoveryConsecutiveNormal);
        recoveryDetails.put("episodeRecovered", true);
        return new DriftEvent(
                null,
                key,
                recoveredAt,
                windowStart,
                recoveredAt,
                DriftEventPhase.RECOVERED,
                direction,
                DriftSeverity.INFO,
                0.0,
                recoveredValue,
                baselineValue,
                detector,
                algorithm,
                "Drift episode recovered after consecutive normal observations",
                tags,
                recoveryDetails
        );
    }

    private static String normalize(String value, String field) {
        return DriftGuardErrors.requireNonBlank(value, field);
    }
}


