package ru.eljke.driftguard.core.config;

import lombok.Builder;
import lombok.Singular;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Транспортно-независимый selector потоков метрик для detector definition.
 *
 * <p>Пустой набор значений означает {@code any}. Например, если заполнено
 * только поле {@code metrics}, detector применяется ко всем сервисам, но только
 * к указанным метрикам.</p>
 *
 * @param services допустимые значения {@code MetricKey.service}
 * @param metrics допустимые значения {@code MetricKey.metric}
 * @param operations допустимые значения {@code MetricKey.operation}
 * @param instances допустимые значения {@code MetricKey.instance}
 */
@Builder(toBuilder = true)
public record MetricSelector(
        @Singular("service")
        Set<String> services,
        @Singular("metric")
        Set<String> metrics,
        @Singular("operation")
        Set<String> operations,
        @Singular("instance")
        Set<String> instances
) implements Predicate<MetricKey> {
    public static final MetricSelector ANY = new MetricSelector(Set.of(), Set.of(), Set.of(), Set.of());

    public MetricSelector {
        services = normalize(services);
        metrics = normalize(metrics);
        operations = normalize(operations);
        instances = normalize(instances);
    }

    public static MetricSelector of(
            List<String> services,
            List<String> metrics,
            List<String> operations,
            List<String> instances
    ) {
        return new MetricSelector(
                normalizeList(services),
                normalizeList(metrics),
                normalizeList(operations),
                normalizeList(instances)
        );
    }

    @Override
    public boolean test(MetricKey key) {
        if (key == null) {
            return false;
        }
        return matches(services, key.service())
                && matches(metrics, key.metric())
                && matches(operations, key.operation())
                && matches(instances, key.instance());
    }

    private static boolean matches(Set<String> expected, String actual) {
        return expected.isEmpty() || expected.contains(actual);
    }

    private static Set<String> normalize(Set<String> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private static Set<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }
}
