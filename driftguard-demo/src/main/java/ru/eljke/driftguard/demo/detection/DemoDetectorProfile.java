package ru.eljke.driftguard.demo.detection;

import java.util.Locale;

/**
 * Профиль чувствительности demo detector-ов.
 */
public enum DemoDetectorProfile {
    AGGRESSIVE,
    BALANCED,
    CONSERVATIVE;

    public static DemoDetectorProfile parse(String value) {
        if (value == null || value.isBlank()) {
            return BALANCED;
        }
        return DemoDetectorProfile.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
