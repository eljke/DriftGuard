package ru.eljke.driftguard.core.error;

/**
 * Machine-readable DriftGuard error reason.
 *
 * <p>{@link #code()} is stable and suitable for logs, APIs and alerts.
 * {@link #description()} stores the message template. The {@link #format(Object...)}
 * method supports placeholders such as {@code {}} and {@code %s}, so enum implementations
 * can stay short and readable.</p>
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


