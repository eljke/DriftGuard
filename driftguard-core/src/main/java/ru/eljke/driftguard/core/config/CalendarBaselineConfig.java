package ru.eljke.driftguard.core.config;

import ru.eljke.driftguard.core.error.DriftGuardErrors;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Calendar-aware baseline partitioning for detector runtime state.
 *
 * <p>When enabled, detector state is kept independently for calendar slots.
 * This lets daily or weekly cyclic traffic be compared with the same calendar
 * regime instead of mixing night, daytime and weekday behavior into one
 * baseline.</p>
 */
public record CalendarBaselineConfig(
        CalendarBaselineMode mode,
        ZoneId zoneId
) {
    public static final CalendarBaselineConfig DISABLED = new CalendarBaselineConfig(
            CalendarBaselineMode.DISABLED,
            ZoneId.of("UTC")
    );

    public CalendarBaselineConfig {
        mode = mode == null ? CalendarBaselineMode.DISABLED : mode;
        zoneId = zoneId == null ? ZoneId.of("UTC") : zoneId;
    }

    public boolean enabled() {
        return mode != CalendarBaselineMode.DISABLED;
    }

    public String slot(Instant timestamp) {
        DriftGuardErrors.requireNonNull(timestamp, "timestamp");
        ZonedDateTime zoned = timestamp.atZone(zoneId);
        return switch (mode) {
            case DISABLED -> "all";
            case HOUR_OF_DAY -> "hour-%02d".formatted(zoned.getHour());
            case HOUR_OF_WEEK -> "%s-hour-%02d".formatted(day(zoned.getDayOfWeek()), zoned.getHour());
        };
    }

    private static String day(DayOfWeek dayOfWeek) {
        return dayOfWeek.name().toLowerCase();
    }
}
