package ru.eljke.driftguard.core.config;

import lombok.Builder;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.Instant;
import java.util.function.Predicate;

/**
 * Runtime binding between a named detector instance, its typed
 * configuration and the metric keys it applies to.
 *
 * <p>Several definitions can refer to the same algorithm while having
 * different thresholds or metric filters.</p>
 *
 * @param name configured detector instance name
 * @param config typed algorithm configuration
 * @param appliesTo predicate that decides whether a metric stream should be processed
 * @param calendarBaseline optional calendar regime used to isolate detector state
 */
@Builder(toBuilder = true)
public record DetectorDefinition(
        String name,
        DetectorConfig config,
        Predicate<MetricKey> appliesTo,
        EmissionPolicyConfig emissionPolicy,
        CalendarBaselineConfig calendarBaseline
) {
    public DetectorDefinition {
        name = normalize(name, "name");
        config = DriftGuardErrors.requireNonNull(config, "config");
        appliesTo = appliesTo == null ? key -> true : appliesTo;
        emissionPolicy = emissionPolicy == null ? EmissionPolicyConfig.DEFAULT : emissionPolicy;
        calendarBaseline = calendarBaseline == null ? CalendarBaselineConfig.DISABLED : calendarBaseline;
    }

    public DetectorDefinition(String name, DetectorConfig config, Predicate<MetricKey> appliesTo) {
        this(name, config, appliesTo, EmissionPolicyConfig.DEFAULT);
    }

    public DetectorDefinition(
            String name,
            DetectorConfig config,
            Predicate<MetricKey> appliesTo,
            EmissionPolicyConfig emissionPolicy
    ) {
        this(name, config, appliesTo, emissionPolicy, CalendarBaselineConfig.DISABLED);
    }

    public boolean matches(MetricKey key) {
        return appliesTo.test(key);
    }

    public String stateDetectorName(Instant timestamp) {
        if (!calendarBaseline.enabled()) {
            return name;
        }
        return name + "@calendar:" + calendarBaseline.slot(timestamp);
    }

    private static String normalize(String value, String field) {
        return DriftGuardErrors.requireNonBlank(value, field);
    }
}

