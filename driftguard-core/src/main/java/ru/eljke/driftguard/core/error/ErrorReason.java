package ru.eljke.driftguard.core.error;

/**
 * Машиночитаемая причина ошибки DriftGuard.
 *
 * <p>{@link #code()} стабилен и подходит для логов, API и alert-ов.
 * {@link #description()} хранит шаблон сообщения. Метод {@link #format(Object...)}
 * поддерживает placeholders вида {@code {}} и {@code %s}, чтобы enum-реализации
 * могли оставаться короткими и удобными для чтения.</p>
 */
public interface ErrorReason {
    String code();

    String description();

    default String format(Object... args) {
        String template = description();
        Object[] safeArgs = args == null ? new Object[0] : args;
        for (Object arg : safeArgs) {
            String value = String.valueOf(arg);
            if (template.contains("{}")) {
                template = template.replaceFirst("\\{}", escapeReplacement(value));
            } else if (template.contains("%s")) {
                template = template.replaceFirst("%s", escapeReplacement(value));
            }
        }
        return template;
    }

    private static String escapeReplacement(String value) {
        return value.replace("\\", "\\\\").replace("$", "\\$");
    }
}
