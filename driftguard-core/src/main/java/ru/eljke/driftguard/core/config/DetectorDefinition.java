package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Runtime-связка между именованным экземпляром detector-а, его типизированной
 * конфигурацией и ключами метрик, к которым он применяется.
 *
 * <p>Несколько definitions могут ссылаться на один и тот же алгоритм, но иметь
 * разные пороги или фильтры метрик.</p>
 *
 * @param name имя настроенного экземпляра detector-а
 * @param config типизированная конфигурация алгоритма
 * @param appliesTo predicate, определяющий, нужно ли обрабатывать поток метрик
 */
public record DetectorDefinition(
        String name,
        DetectorConfig config,
        Predicate<MetricKey> appliesTo,
        EmissionPolicyConfig emissionPolicy
) {
    public DetectorDefinition {
        name = normalize(name, "name");
        config = Objects.requireNonNull(config, "config must not be null");
        appliesTo = appliesTo == null ? key -> true : appliesTo;
        emissionPolicy = emissionPolicy == null ? EmissionPolicyConfig.DEFAULT : emissionPolicy;
    }

    public DetectorDefinition(String name, DetectorConfig config, Predicate<MetricKey> appliesTo) {
        this(name, config, appliesTo, EmissionPolicyConfig.DEFAULT);
    }

    public boolean matches(MetricKey key) {
        return appliesTo.test(key);
    }

    private static String normalize(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
