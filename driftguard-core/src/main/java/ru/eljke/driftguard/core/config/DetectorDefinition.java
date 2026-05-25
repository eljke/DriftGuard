package ru.eljke.driftguard.core.config;

import lombok.Builder;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

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
@Builder(toBuilder = true)
public record DetectorDefinition(
        String name,
        DetectorConfig config,
        Predicate<MetricKey> appliesTo,
        EmissionPolicyConfig emissionPolicy
) {
    public DetectorDefinition {
        name = normalize(name, "name");
        config = DriftGuardErrors.requireNonNull(config, "config");
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
        return DriftGuardErrors.requireNonBlank(value, field);
    }
}
